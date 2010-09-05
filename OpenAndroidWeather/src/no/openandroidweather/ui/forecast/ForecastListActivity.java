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

import java.util.ArrayList;
import java.util.List;

import no.openandroidweather.R;
import no.openandroidweather.misc.IProgressItem;
import no.openandroidweather.weatherservice.IForecastEventListener;
import no.openandroidweather.weatherservice.IWeatherService;
import no.openandroidweather.weatherservice.WeatherService;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ListView.FixedViewInfo;
import android.widget.ProgressBar;

public class ForecastListActivity extends ListActivity implements IProgressItem {
	/**
	 * Set this in intent extra to show a specific place. Use row id from
	 * WeatherContentProvider.Places
	 */
	public static final String _ROW_ID = "_rowId";
	private final String TAG = "ForecastListActivity";
	private final IForecastEventListener forcastListener = new ForecastListener();
	private ProgressBar progressBar;
	private final Handler mHandler = new Handler();
	private IWeatherService mService;

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(final ComponentName name,
				final IBinder service) {
			mService = IWeatherService.Stub.asInterface(service);
			synchronized (mService) {
				// Sets up a call for new forecast
				try {
					mService.getNearestForecast(forcastListener, 2000, 100);
				} catch (final RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			if (mService != null)
				synchronized (mService) {
					mService = null;
				}
		}

	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.forecast_view);
		progressBar = (ProgressBar) findViewById(R.id.progressbar);
	}

	@Override
	protected void onPause() {
		unbindService(mServiceConnection);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mService == null)
			bindService(new Intent(getApplicationContext(),
					WeatherService.class), mServiceConnection, BIND_AUTO_CREATE);
	}

	/**
	 * @param Between
	 *            1 and 1000
	 */
	@Override
	public void progress(final int progress) {
		progressBar.setProgress(progress);
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
				builder.setMessage(getResources().getString(
						R.string.no_internet_connection));
				break;
			case WeatherService.ERROR_NO_KNOWN_POSITION:
				builder.setMessage(getResources().getString(
						R.string.no_position));
				break;
			case WeatherService.ERROR_UNKNOWN_ERROR:
			default:
				builder.setMessage(getResources().getString(
						R.string.unknown_error_while_downloading));
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
			ForecastListActivity.this.progress(500);
			hasGotForecast = true;
			new RenderContentTask().execute(Uri.parse(uri));
		}

		@Override
		public void progress(final int progress) throws RemoteException {
			if (!hasGotForecast)
				ForecastListActivity.this.progress(progress / 2);
		}

	}

	private class RenderContentTask extends AsyncTask<Uri, Float, ListAdapter> {

		@Override
		protected ListAdapter doInBackground(final Uri... params) {
			final ForecastListParser parser = new ForecastListParser(
					getApplicationContext(), ForecastListActivity.this);
			final List<IListRow> rows = parser.parseData(params[0]);
			final ListView.FixedViewInfo header = getListView().new FixedViewInfo();
			header.view = parser.getHeaderView(params[0]);
			final ArrayList<FixedViewInfo> headerViewInfos = new ArrayList<ListView.FixedViewInfo>();
			headerViewInfos.add(header);
			return new HeaderViewListAdapter(headerViewInfos, null,
					new ForecastListAdapter(rows));
		}

		@Override
		protected void onPostExecute(final ListAdapter result) {
			super.onPostExecute(result);
			setListAdapter(result);
		}

	}
}
