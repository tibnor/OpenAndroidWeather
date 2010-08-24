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
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Intent intent = new Intent(context, UpdateService.class);
		context.startService(intent);
	}

	public static class UpdateService extends Service {
		private ForecastListener forcastListener;
		private IWeatherService mService;
		private boolean isWorking = true;
		private ServiceConnection mServiceConnection = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				// Debug.waitForDebugger();
				mService = IWeatherService.Stub.asInterface(service);
				// Sets up a call for new forecast
				try {
					mService.getNearestForecast(forcastListener, 2000, 100);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				mService = null;
			}

		};

		@Override
		public void onStart(Intent intent, int startId) {
			// Bind to service
			Log.i(TAG, "Updating weather widget");
			forcastListener = new ForecastListener();

			if (mService == null) {
				bindService(new Intent(getApplicationContext(),
						WeatherService.class), mServiceConnection,
						BIND_AUTO_CREATE);
			} else {
				try {
					mService.getNearestForecast(forcastListener, 2000, 100);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		private void updateWidget(Uri contentUri) {

			Context context = getApplicationContext();
			RemoteViews view = new RemoteViews(context.getPackageName(),
					R.layout.widget);
			ContentResolver cr = getContentResolver();

			String projection[] = { WeatherContentProvider.META_NEXT_FORECAST,
					WeatherContentProvider.META_LOADED };
			Cursor c = cr.query(contentUri, projection, null, null, null);
			c.moveToFirst();

			java.text.DateFormat df = DateFormat.getTimeFormat(context);

			String nextForecast = "Next: ";
			long nextForecastI = c
					.getLong(c
							.getColumnIndexOrThrow(WeatherContentProvider.META_NEXT_FORECAST));
			nextForecast += df.format(new Date(nextForecastI));
			view.setTextViewText(R.id.widget_next_forecast, nextForecast);

			String loaded = "Downloaded: ";
			long loadedL = c.getLong(c
					.getColumnIndexOrThrow(WeatherContentProvider.META_LOADED));

			loaded += df.format(new Date(loadedL));
			view.setTextViewText(R.id.widget_updated, loaded);
			c.close();

			// Add onClick intent for updating:
			PendingIntent pendingIntent = PendingIntent.getService(context, 0,
					new Intent(context, UpdateService.class),
					PendingIntent.FLAG_UPDATE_CURRENT);
			view.setOnClickPendingIntent(R.id.widget_background, pendingIntent);

			// Adding weather data
			contentUri = Uri.withAppendedPath(contentUri,
					WeatherContentProvider.FORECAST_CONTENT_DIRECTORY);
			long timeStart = System.currentTimeMillis();
			Log.d(TAG, "System time" + timeStart);
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
			ComponentName thisWidget = new ComponentName(this,
					WidgetProvider.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(this);

			manager.updateAppWidget(thisWidget, view);
			if (forcastListener.isComplete()) {
				stop();
			}
		}

		public void stop() {
			// Make an alarm for next update
			Context context = getApplicationContext();
			PendingIntent pendingIntent = PendingIntent.getService(context, 0,
					new Intent(context, UpdateService.class),
					PendingIntent.FLAG_ONE_SHOT);
			AlarmManager alarm = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			// Set time to next hour:
			long triggerAtTime = System.currentTimeMillis();
			// Mid step set to last hour:
			triggerAtTime -= triggerAtTime % 3600000;
			triggerAtTime += 3600000;

			alarm.set(AlarmManager.RTC, triggerAtTime, pendingIntent);
			stopSelf();
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
		}

		/**
		 * @param contentUri
		 * @param view
		 * @param cr
		 * @param timeStart
		 * @param timeStop
		 * @param i
		 */
		private void getAndInsertData(Uri contentUri, RemoteViews view,
				ContentResolver cr, long timeStart, long timeStop, int i) {
			// Set time:
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timeStart);
			int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2;
			if (dayOfWeek < 0) {
				dayOfWeek += 7;
			}
			String time = WEEK_DAYS[dayOfWeek] + "";
			time += " " + cal.get(Calendar.HOUR_OF_DAY);
			view.setTextViewText(Q.time[i], time);

			// Gets symbol and precipitation
			String selection = WeatherContentProvider.FORECAST_FROM + "="
					+ timeStart + " AND " + WeatherContentProvider.FORECAST_TO
					+ "=" + timeStop;
			String[] projection = { WeatherContentProvider.FORECAST_TYPE,
					WeatherContentProvider.FORECAST_VALUE };
			Cursor c = cr.query(contentUri, projection, selection, null, null);
			c.moveToFirst();
			int length = c.getCount();
			int typeCol = c
					.getColumnIndexOrThrow(WeatherContentProvider.FORECAST_TYPE);
			int valueCol = c
					.getColumnIndexOrThrow(WeatherContentProvider.FORECAST_VALUE);
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
					Integer weatherType = new Integer(value);
					Log.d(TAG, "Time: " + time + "symbol:" + weatherType);
					view.setImageViewResource(Q.image[i], Q.icon[weatherType]);
					break;
				}
				c.moveToNext();
			}
			c.close();

			selection = WeatherContentProvider.FORECAST_FROM + "=" + timeStart
					+ " AND " + WeatherContentProvider.FORECAST_TO + "="
					+ timeStart;
			c = cr.query(contentUri, projection, selection, null, null);
			c.moveToFirst();
			length = c.getCount();
			typeCol = c
					.getColumnIndexOrThrow(WeatherContentProvider.FORECAST_TYPE);
			valueCol = c
					.getColumnIndexOrThrow(WeatherContentProvider.FORECAST_VALUE);
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
		public IBinder onBind(Intent intent) {
			return null;
		}

		private class ForecastListener extends IForecastEventListener.Stub {
			private boolean isComplete;

			@Override
			public void newForecast(String uri, long forecastGenerated)
					throws RemoteException {
				updateWidget(Uri.parse(uri));
				isComplete = false;
			}

			@Override
			public void progress(double progress) throws RemoteException {
				// TODO Show progress?
			}

			@Override
			public void newExpectedTime() throws RemoteException {
				// TODO Show expected?
			}

			@Override
			public void exceptionOccurred(int errorcode) throws RemoteException {
				String text;
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
				Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG)
						.show();
			}

			public boolean isComplete() {
				return isComplete;
			}

			@Override
			public void completed() throws RemoteException {
				if (mServiceConnection != null)
					unbindService(mServiceConnection);
				isComplete = true;
				if (isWorking)
					stop();
			}
		}
	}
}
