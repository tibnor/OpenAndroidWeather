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

package no.openandroidweather.widget;

import java.util.Calendar;
import java.util.Date;

import no.openandroidweather.R;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weathercontentprovider.WeatherType;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.Forecast;
import no.openandroidweather.weatherservice.IForecastEventListener;
import no.openandroidweather.weatherservice.IWeatherService;
import no.openandroidweather.weatherservice.WeatherService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

//Pattern from http://developer.android.com/resources/samples/WiktionarySimple/src/com/example/android/simplewiktionary/WordWidget.html
public class WidgetProvider extends AppWidgetProvider {
	private static final String WEEK_DAYS[] = { "man", "tir", "ons", "tor",
			"fre", "lør", "søn" };

	private static final String TAG = "no.openAndroidWeahter.WidgetProvider";

	@Override
	public void onDisabled(final Context context) {
		super.onDisabled(context);
		Log.i(TAG, "Last widget removed.");

		// Remove alarm
		final PendingIntent pendingIntent = PendingIntent.getService(context,
				0, new Intent(context, UpdateService.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		final AlarmManager alarm = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pendingIntent);

	}

	@Override
	public void onEnabled(final Context context) {
		super.onEnabled(context);

		Log.i(TAG, "First widget added.");

		// Set alarm
		final PendingIntent pendingIntent = PendingIntent.getService(context,
				0, new Intent(context, UpdateService.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		final AlarmManager alarm = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		// Set time to next hour plus 5 minutes:
		long triggerAtTime = System.currentTimeMillis();
		// Mid step set to last hour:
		triggerAtTime -= triggerAtTime % 3600000;
		triggerAtTime += 65 * 60 * 1000;
		alarm.setInexactRepeating(AlarmManager.RTC, triggerAtTime,
				AlarmManager.INTERVAL_HOUR, pendingIntent);
	}

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		final Intent intent = new Intent(context, UpdateService.class);
		context.startService(intent);
	}

	public static class UpdateService extends Service {
		private static boolean isAlreadyRunning = false;
		private ForecastListener forcastListener;
		private IWeatherService mService;

		/**
		 * Is true if a call to WeatherService is sent and updateWidget is not
		 * completed
		 */
		private boolean isWorking = false;
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

		/**
		 * @param contentUri
		 * @param view
		 * @param cr
		 * @param timeStart
		 * @param timeStop
		 * @param i
		 */
		private void getAndInsertData(final Uri contentUri,
				final RemoteViews view, final ContentResolver cr,
				final long timeStart, final long timeStop, final int i) {
			// Set time:
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timeStart);
			int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2;
			if (dayOfWeek < 0)
				dayOfWeek += 7;
			String time = WEEK_DAYS[dayOfWeek] + "";
			time += " " + cal.get(Calendar.HOUR_OF_DAY);
			view.setTextViewText(Q.time[i], time);

			// Gets symbol and precipitation
			String selection = WeatherContentProvider.Forecast.FROM + "="
					+ timeStart + " AND " + WeatherContentProvider.Forecast.TO
					+ "=" + timeStop;
			final String[] projection = { WeatherContentProvider.Forecast.TYPE,
					WeatherContentProvider.Forecast.VALUE };
			Cursor c = cr.query(contentUri, projection, selection, null, null);
			c.moveToFirst();
			int length = c.getCount();
			int typeCol = c
					.getColumnIndexOrThrow(WeatherContentProvider.Forecast.TYPE);
			int valueCol = c
					.getColumnIndexOrThrow(WeatherContentProvider.Forecast.VALUE);
			int type;
			String value;
			for (int j = 0; j < length; j++) {
				type = c.getInt(typeCol);
				value = c.getString(valueCol);
				switch (type) {
				case WeatherType.precipitation:
					view.setTextViewText(Q.pert[i], value);
					break;
				case WeatherType.symbol:
					final Integer weatherType = new Integer(value);
					Log.d(TAG, "Time: " + time + "symbol:" + weatherType);
					view.setImageViewResource(Q.image[i], Q.symbol[weatherType]);
					break;
				}
				c.moveToNext();
			}
			c.close();

			selection = WeatherContentProvider.Forecast.FROM + "=" + timeStart
					+ " AND " + WeatherContentProvider.Forecast.TO + "="
					+ timeStart;
			c = cr.query(contentUri, projection, selection, null, null);
			c.moveToFirst();
			length = c.getCount();
			typeCol = c
					.getColumnIndexOrThrow(WeatherContentProvider.Forecast.TYPE);
			valueCol = c
					.getColumnIndexOrThrow(WeatherContentProvider.Forecast.VALUE);
			for (int j = 0; j < length; j++) {
				type = c.getInt(typeCol);
				value = c.getString(valueCol);
				switch (type) {
				case WeatherType.temperature:
					view.setTextViewText(Q.temp[i], value);
					break;
				}
				c.moveToNext();
			}
			c.close();

		}

		@Override
		public IBinder onBind(final Intent intent) {
			return null;
		}

		@Override
		public void onStart(final Intent intent, final int startId) {
			// Bind to service
			if (isAlreadyRunning) {
				Log.i(TAG, "Updating is ongoing, new update ignored");
				return;
			} else {
				isAlreadyRunning = true;
				isWorking = true;
				Log.i(TAG, "Updating weather widget");

				if (mService == null) {
					forcastListener = new ForecastListener();
					bindService(new Intent(getApplicationContext(),
							WeatherService.class), mServiceConnection,
							BIND_AUTO_CREATE);
				} else
					synchronized (mService) {
						try {
							mService.getNearestForecast(forcastListener, 2000,
									100);
						} catch (final RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
			}

		}

		public void stop() {
			Log.i(TAG, "stopping");
			isAlreadyRunning = false;

			stopSelf();
		}

		private void updateWidget(Uri contentUri) {
			Log.i(TAG, "Rendering widget");
			final Context context = getApplicationContext();
			final RemoteViews view = new RemoteViews(context.getPackageName(),
					R.layout.widget);
			final ContentResolver cr = getContentResolver();

			final String projection[] = {
					WeatherContentProvider.Meta.NEXT_FORECAST,
					WeatherContentProvider.Meta.LOADED };
			final Cursor c = cr.query(contentUri, projection, null, null, null);
			c.moveToFirst();

			final java.text.DateFormat df = DateFormat.getTimeFormat(context);

			String nextForecast = "Next: ";
			final long nextForecastI = c
					.getLong(c
							.getColumnIndexOrThrow(WeatherContentProvider.Meta.NEXT_FORECAST));
			nextForecast += df.format(new Date(nextForecastI));
			view.setTextViewText(R.id.widget_next_forecast, nextForecast);

			String loaded = "Downloaded: ";
			final long loadedL = c.getLong(c
					.getColumnIndexOrThrow(WeatherContentProvider.Meta.LOADED));

			loaded += df.format(new Date(loadedL));
			view.setTextViewText(R.id.widget_updated, loaded);
			c.close();

			// Add onClick intent for updating:
			final PendingIntent pendingIntent = PendingIntent.getService(
					context, 0, new Intent(context, UpdateService.class),
					PendingIntent.FLAG_UPDATE_CURRENT);
			view.setOnClickPendingIntent(R.id.widget_background, pendingIntent);

			// Adding weather data
			contentUri = Uri.withAppendedPath(contentUri,
					WeatherContentProvider.Forecast.CONTENT_DIRECTORY);
			long timeStart = System.currentTimeMillis();
			// Rounds down to last hour
			timeStart -= timeStart % 3600000;
			long timeStop = timeStart + 3600000;
			int i;
			// Goes thru first the first 9 points, where it is an hour between
			// each point.
			for (i = 0; i < 9; i++) {
				getAndInsertData(contentUri, view, cr, timeStart, timeStop, i);
				timeStart = timeStop;
				timeStop += 3600000;
			}

			// Goes thru first the next 4 points, where it is 2 hour between
			// each point.
			for (; i < 13; i++) {
				getAndInsertData(contentUri, view, cr, timeStart, timeStop, i);
				timeStart = timeStop;
				timeStop += 3600000 * 2;
			}

			// Goes thru first the last 5 points, where it is 3 hour between
			// each point.
			for (; i < 18; i++) {
				getAndInsertData(contentUri, view, cr, timeStart, timeStop, i);
				timeStart = timeStop;
				timeStop += 3600000 * 3;
			}

			// Push update for this widget to the home screen
			final ComponentName thisWidget = new ComponentName(this,
					WidgetProvider.class);
			final AppWidgetManager manager = AppWidgetManager.getInstance(this);

			manager.updateAppWidget(thisWidget, view);
			isWorking = false;
			if (forcastListener.isComplete())
				stop();
		}

		private class ForecastListener extends IForecastEventListener.Stub {
			private final Handler mHandler = new Handler();
			private boolean isComplete;

			@Override
			public void completed() throws RemoteException {
				if (mService != null) {
					synchronized (mService) {
						try {
							unbindService(mServiceConnection);
						} catch (final IllegalArgumentException e) {
							// The service is unbind before binding
						}
						mService = null;
					}
				}
				isComplete = true;
				if (!isWorking)
					stop();

			}

			@Override
			public void exceptionOccurred(final int errorcode)
					throws RemoteException {
				final String text;
				switch (errorcode) {
				case (WeatherService.ERROR_NETWORK_ERROR):
					text = "Check internett connection, can't download forecast!";
					break;
				case (WeatherService.ERROR_NO_KNOWN_POSITION):
					text = "There is no known position, can't download forecast!";
					break;
				case (WeatherService.ERROR_UNKNOWN_ERROR):
				default:
					text = "Trouble getting forecast, sorry!";
				}
				Thread ShowToastThread = new Thread() {
					public void run() {
						Toast.makeText(getApplicationContext(), text,
								Toast.LENGTH_LONG).show();
					};
				};
				Log.e(TAG, "Error in widgetProvider:" + text);
				mHandler.post(ShowToastThread);
				completed();

			}

			public boolean isComplete() {
				return isComplete;
			}

			@Override
			public void newExpectedTime() throws RemoteException {
				// TODO Show expected?
			}

			@Override
			public void newForecast(final String uri,
					final long forecastGenerated) throws RemoteException {
				updateWidget(Uri.parse(uri));
				isComplete = false;
			}

			@Override
			public void progress(final int progress) throws RemoteException {
				// TODO Show progress?
			}
		}
	}
}
