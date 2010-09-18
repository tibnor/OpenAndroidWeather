/*
	Copyright 2010 Torstein Ingebrigtsen Bø

    This file is part of OpenAndroidWeather.

    OpenAndroidWeather is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenAndroidWeather is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenAndroidWeather.  If not, see <http://www.gnu.org/licenses/>.
 */

package no.openandroidweather.ui.forecast;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import no.openandroidweather.R;
import no.openandroidweather.misc.IProgressItem;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.ForecastListView;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.Place;
import no.openandroidweather.weatherservice.IForecastEventListener;
import no.openandroidweather.weatherservice.IWeatherService;
import no.openandroidweather.weatherservice.WeatherService;
import no.openandroidweather.widget.Q;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ForecastListActivity extends Activity implements IProgressItem {
	/**
	 * Set this in intent extra to show a specific place. Use row id from
	 * WeatherContentProvider.Places
	 */
	public static final String PLACE_ROW_ID = "_rowId";
	private final String TAG = "ForecastListActivity";
	private final IForecastEventListener forcastListener = new ForecastListener();
	private ProgressDialog mProgressBar;
	private TableLayout mTable;
	private final Handler mHandler = new Handler();
	private IWeatherService mService = null;
	private LayoutInflater inflater;
	final DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
	private Queue<View> mTableRows = new ConcurrentLinkedQueue<View>();

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(final ComponentName name,
				final IBinder service) {
			mService = IWeatherService.Stub.asInterface(service);

			// Sets up a call for new forecast
			Bundle extra = getIntent().getExtras();
			boolean getFromPlace = false;
			if (extra != null) {
				long placeId = extra.getLong(PLACE_ROW_ID, -1L);
				if (placeId != -1L) {
					getFromPlace = true;
					// Gets place in database for the forecast
					Uri uri = Uri.withAppendedPath(Place.CONTENT_URI, placeId
							+ "");
					String[] projection = { Place.FORECAST_ROW };
					Cursor c = getContentResolver().query(uri, projection,
							null, null, null);
					c.moveToFirst();
					int forecastId = c.getInt(c
							.getColumnIndex(Place.FORECAST_ROW));
					c.close();

					// Make the new uri for the forecast.
					uri = Uri
							.withAppendedPath(
									WeatherContentProvider.CONTENT_URI,
									forecastId + "");

					try {
						forcastListener.newForecast(uri.toString(), 0L);
						forcastListener.completed();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}

			if (!getFromPlace) {
				try {
					mService.getNearestForecast(forcastListener, 2000, 100);
				} catch (final RemoteException e) {
					e.printStackTrace();
				}
			}

		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			if (mService != null)
				synchronized (mService) {
					if (mService != null)
						mService = null;
				}
		}

	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.forecast_view);
		// progressBar = (ProgressBar) findViewById(R.id.progressbar);
		mTable = (TableLayout) findViewById(R.id.forecastsTable);
		mTable.setStretchAllColumns(true);
		bindService(new Intent(getApplicationContext(), WeatherService.class),
				mServiceConnection, BIND_AUTO_CREATE);

		inflater = getLayoutInflater();

		mProgressBar = new ProgressDialog(this);
		mProgressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressBar.setMax(1000);
		mProgressBar.setMessage(getResources().getText(
				R.string.please_wait_1min));
		mProgressBar.setCancelable(false);
		mProgressBar.show();

	}

	@Override
	protected void onDestroy() {
		if (mService != null) {
			synchronized (mService) {
				if (mService != null)
					unbindService(mServiceConnection);
			}
		}

		super.onPause();
	}

	/**
	 * @param Between
	 *            1 and 1000
	 */
	@Override
	public void progress(final int progress) {
		mProgressBar.setProgress(progress);
	}

	private void checkDate(final long from, final long to) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(from);
		final int oldDate = cal.get(Calendar.DATE);
		cal.setTimeInMillis(to);
		final int newDate = cal.get(Calendar.DATE);
		if (oldDate != newDate)
			inflateDateRow(to);

	}

	public void inflateDateRow(long date) {
		final View row = inflater.inflate(R.layout.forecast_view_date_item,
				null);
		final View descriptionRow = inflater.inflate(
				R.layout.forecast_view_description, null);
		final View unitsRow = inflater.inflate(R.layout.forecast_view_units,
				null);

		final DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
		String dateS = df.format(new Date(date));
		((TextView) row).setText(dateS);

		mTableRows.add(row);
		mTableRows.add(descriptionRow);
		mTableRows.add(unitsRow);
	}

	/**
	 * Get the meta data and render the header
	 * 
	 * @param uri
	 * @return If no forecast it returns false else true
	 */
	public boolean setHeader(final Uri uri) {
		final Cursor c = getContentResolver()
				.query(uri, null, null, null, null);
		if (c == null || c.getCount() == 0) {
			c.close();
			noForecast();
			return false;
		}

		c.moveToFirst();
		final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT);

		final String place = c.getString(c
				.getColumnIndex(WeatherContentProvider.Meta.PLACE_NAME));
		final long downloaded = c.getLong(c
				.getColumnIndex(WeatherContentProvider.Meta.LOADED));
		final long generated = c.getLong(c
				.getColumnIndexOrThrow(WeatherContentProvider.Meta.GENERATED));
		final long nextForecast = c
				.getLong(c
						.getColumnIndexOrThrow(WeatherContentProvider.Meta.NEXT_FORECAST));
		c.close();

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				((TextView) findViewById(R.id.place)).setText(place);

				((TextView) findViewById(R.id.downloaded)).setText(df
						.format(new Date(downloaded)));

				((TextView) findViewById(R.id.generated)).setText(df
						.format(new Date(generated)));

				((TextView) findViewById(R.id.next_update)).setText(df
						.format(new Date(nextForecast)));
			}
		});
		return true;

	}

	public void parseData(Uri uri, View mainView) {
		Uri forecastUri = Uri.withAppendedPath(uri,
				ForecastListView.CONTENT_PATH);

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mProgressBar = new ProgressDialog(ForecastListActivity.this);
				mProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mProgressBar.setMessage(getResources().getString(
						R.string.please_wait));
				mProgressBar.show();
			}
		});

		if (!setHeader(uri))
			return;

		// Set start time to the beginning of this hour
		long lastHour = System.currentTimeMillis();
		lastHour -= 3600000;

		final String selection = ForecastListView.fromTime + ">" + lastHour;
		final Cursor c = getContentResolver().query(forecastUri, null,
				selection, null, null);

		if (c == null || c.getCount() == 0) {
			c.close();
			noForecast();
			return;
		}

		final int fromCol = c.getColumnIndexOrThrow(ForecastListView.fromTime);
		final int toCol = c.getColumnIndexOrThrow(ForecastListView.toTime);
		final int percipitationCol = c
				.getColumnIndexOrThrow(ForecastListView.percipitation);
		final int symbolCol = c.getColumnIndexOrThrow(ForecastListView.symbol);
		final int temperatureCol = c
				.getColumnIndexOrThrow(ForecastListView.temperature);
		final int windDirectionCol = c
				.getColumnIndexOrThrow(ForecastListView.windDirection);
		final int windSpeedCol = c
				.getColumnIndexOrThrow(ForecastListView.windSpeed);

		c.moveToFirst();
		long from = c.getLong(fromCol), to;
		int symbol;
		double percipitation, temperature, windDirection, windSpeed;
		inflateDateRow(from);
		int length = c.getCount();
		for (int i = 0; i < length; i++) {
			from = c.getLong(fromCol);
			to = c.getLong(toCol);
			symbol = c.getInt(symbolCol);
			percipitation = c.getDouble(percipitationCol);
			temperature = c.getDouble(temperatureCol);
			windDirection = c.getDouble(windDirectionCol);
			windSpeed = c.getDouble(windSpeedCol);
			addForecastRow(symbol, temperature, percipitation, windSpeed,
					windDirection, from);
			checkDate(from, to);
			c.moveToNext();

		}
		c.close();

		dumpTableRows();
		
		mProgressBar.dismiss();

		return;
	}

	private void noForecast() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(
				ForecastListActivity.this);
		builder.setMessage(R.string.no_forecast);
		builder.setCancelable(false);
		builder.setPositiveButton(getResources().getString(R.string.ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						ForecastListActivity.this.finish();
						dialog.cancel();
					}
				});
		final Thread ShowErrorThread = new Thread() {
			@Override
			public void run() {
				builder.show();
			};
		};
		mHandler.post(ShowErrorThread);
	}

	public void addForecastRow(final int symbol, final double temperature,
			final double percipitation, final double windSpeed,
			final double windDirection, final long startTime) {
		final TableRow row = (TableRow) inflater.inflate(
				R.layout.forecast_view_item, null);
		final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
		String hour = df.format(new Date(startTime));

		// Set hour
		((TextView) row.findViewById(R.id.hour)).setText(hour);

		// Set symbol
		((ImageView) row.findViewById(R.id.symbol))
				.setImageResource(Q.symbol[symbol]);

		// Set temperature
		((TextView) row.findViewById(R.id.temperature)).setText(Double
				.toString(temperature));

		// Set precipitation
		((TextView) row.findViewById(R.id.percipitation)).setText(Double
				.toString(percipitation));

		// Set wind speed
		((TextView) row.findViewById(R.id.wind_speed)).setText(Double
				.toString(windSpeed));

		// Set wind direction
		((TextView) row.findViewById(R.id.wind_direction)).setText(Double
				.toString(windDirection));

		mTableRows.add(row);

		return;
	}

	private void dumpTableRows() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				for (View row : mTableRows) {
					mTable.addView(row);
				}
			}
		});

	}

	private class ForecastListener extends IForecastEventListener.Stub {
		boolean hasGotForecast = false;

		@Override
		public void completed() throws RemoteException {
		}

		@Override
		public void exceptionOccurred(final int errorcode)
				throws RemoteException {
			if (ForecastListActivity.this == null || isFinishing())
				return;

			Log.e(TAG,
					"error occured during downloading of forecast, errorcode:"
							+ errorcode);
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					ForecastListActivity.this);
			switch (errorcode) {
			case WeatherService.ERROR_NETWORK_ERROR:
				builder.setMessage(R.string.no_internet_connection);
				break;
			case WeatherService.ERROR_NO_KNOWN_POSITION:
				builder.setMessage(R.string.no_position);
				break;
			case WeatherService.ERROR_NO_WIFI:
				builder.setMessage(R.string.no_wifi_connection);
				break;
			case WeatherService.ERROR_UNKNOWN_ERROR:
			default:
				builder.setMessage(R.string.unknown_error_while_downloading);
				break;
			}
			builder.setCancelable(false);
			builder.setPositiveButton(getResources().getString(R.string.yes),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							try {
								mService.getNearestForecast(
										ForecastListener.this, 2000, 100);
							} catch (final RemoteException e) {
								e.printStackTrace();
							}
							dialog.cancel();
						}
					});
			builder.setNegativeButton(getResources().getString(R.string.no),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							dialog.cancel();
							if (!hasGotForecast)
								finish();
						}
					});
			final Thread ShowErrorThread = new Thread() {
				@Override
				public void run() {
					builder.show();
				};
			};
			mHandler.post(ShowErrorThread);

		}

		@Override
		public void newExpectedTime() throws RemoteException {
			// TODO update data
		}

		@Override
		public void newForecast(final String uri, final long forecastGenerated)
				throws RemoteException {
			hasGotForecast = true;
			mProgressBar.setProgress(1000);
			mProgressBar.dismiss();
			new Thread(new Runnable() {

				@Override
				public void run() {
					parseData(Uri.parse(uri), findViewById(R.id.main));
				}
			}).start();

		}

		@Override
		public void progress(final int progress) throws RemoteException {
			if (!hasGotForecast)
				ForecastListActivity.this.progress(progress);
		}

	}
}
