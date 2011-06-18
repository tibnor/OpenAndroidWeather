/*
	Copyright 2011 Torstein Ingebrigtsen Bø

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

package no.firestorm.weathernotificatonservice;

import java.text.DateFormat;
import java.util.Date;

import no.firestorm.R;
import no.firestorm.misc.TempToDrawable;
import no.firestorm.ui.stationpicker.Station;
import no.firestorm.wsklima.WeatherElement;
import no.firestorm.wsklima.WsKlimaProxy;
import no.firestorm.wsklima.database.WsKlimaDataBaseHelper;
import no.firestorm.wsklima.exception.NoLocationException;

import org.apache.http.HttpException;

import android.accounts.NetworkErrorException;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class WeatherNotificationService extends Service {

	private class UpdateStation implements LocationListener {
		private ShowTempAsync object = null;

		public UpdateStation(ShowTempAsync object) {
			this.object = object;
		}

		@Override
		public void onLocationChanged(Location location) {
			if (location.getAccuracy() < 3000) {
				Context context = WeatherNotificationService.this;
				WsKlimaDataBaseHelper db = new WsKlimaDataBaseHelper(context);
				Station station = db.getStationsSortedByLocation(location).get(
						0);
				WsKlimaProxy.setStationName(context, station.getName(),
						station.getId());
				// Remove listener
				removeFromBroadcaster();
				if (object != null) {
					synchronized (object) {
						object.setStationReady(true);
						object.notify();
					}
				}
			}

		}

		public void removeFromBroadcaster() {
			Context context = WeatherNotificationService.this;
			LocationManager locManager = (LocationManager) context
					.getSystemService(LOCATION_SERVICE);
			locManager.removeUpdates(this);
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			// throw new UnsupportedOperationException("Not implemented!");
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			// throw new UnsupportedOperationException("Not implemented!");
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			// throw new UnsupportedOperationException("Not implemented!");
		}

	}

	private class ShowTempAsync extends AsyncTask<Void, Void, Object> {
		Boolean stationReady = true;

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			Context context = WeatherNotificationService.this;
			if (WsKlimaProxy.getUseNearestStation(context)) {
				// find nearest station
				// find current position
				LocationManager locMan = (LocationManager) context
						.getSystemService(LOCATION_SERVICE);
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_COARSE);
				String provider = locMan.getBestProvider(criteria, true);
				UpdateStation updateStation = new UpdateStation(this);
				locMan.requestLocationUpdates(provider, 0, 0, updateStation);
				stationReady = false;
			}
		}

		@Override
		protected Object doInBackground(Void... dummy) {
			synchronized (this) {
				if (!stationReady) {
					try {
						 this.wait(2 * 60 * 1000);
						// TODO change back
						//this.wait(1);
					} catch (InterruptedException e) {
						return new NoLocationException(e);
					}
				}
				if (!stationReady)
					return new NoLocationException(null);
			}
			// get temp
			final WsKlimaProxy weatherProxy = new WsKlimaProxy();
			try {
				return weatherProxy
						.getTemperatureNow(WeatherNotificationService.this);
			} catch (final NetworkErrorException e) {
				return e;
			} catch (final HttpException e) {
				return e;
			}
		}

		void setStationReady(boolean ready) {
			stationReady = ready;
		}

		@Override
		protected void onPostExecute(Object object) {
			super.onPostExecute(object);
			int tickerIcon, contentIcon;
			CharSequence tickerText, contentTitle, contentText, contentTime;
			final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
			if (object instanceof WeatherElement) {
				// Has data
				final WeatherElement temperature = (WeatherElement) object;
				// Find name
				final String stationName = WsKlimaProxy
						.getStationName(WeatherNotificationService.this);

				// Find icon
				tickerIcon = TempToDrawable.getDrawableFromTemp(Float
						.valueOf(temperature.getValue()));
				contentIcon = tickerIcon;
				// Set title
				tickerText = stationName;
				contentTitle = stationName;
				contentTime = df.format(temperature.getFrom());

				final Context context = WeatherNotificationService.this;
				contentText = String.format("%s %.1f °C", context
						.getString(R.string.temperatur_),
						new Float(temperature.getValue()));
				updateAlarm();

			} else {
				// Find icon
				contentIcon = android.R.drawable.stat_notify_error;
				Context context = WeatherNotificationService.this;
				contentTime = df.format(new Date());

				if (object == null) {
					// No data
					// Set text
					tickerText = context.getText(R.string.no_available_data);
					contentTitle = context.getText(R.string.no_available_data);
					contentText = context
							.getString(R.string.try_another_station);
					tickerIcon = android.R.drawable.stat_notify_error;
				} else {
					Exception e = (Exception) object;
					if (e instanceof NoLocationException) {
						tickerText = WeatherNotificationService.this
								.getString(R.string.location_error);
						contentTitle = WeatherNotificationService.this
								.getString(R.string.location_error);
					} else {

						// Network error has occurred
						// Set title
						tickerText = WeatherNotificationService.this
								.getString(R.string.download_error);
						contentTitle = WeatherNotificationService.this
								.getString(R.string.download_error);
					}

					Date lastTime = WsKlimaProxy
							.getLastUpdateTime(WeatherNotificationService.this);
					if (lastTime != null) {
						Float temperatureF = Float.parseFloat(WsKlimaProxy
								.getLastTemperature(context));
						contentText = String.format("%s %.1f °C %s %s",
								context.getString(R.string.last_temperature),
								temperatureF,
								context.getString(R.string._tid_),
								df.format(lastTime));
						tickerIcon = TempToDrawable
								.getDrawableFromTemp(temperatureF);

					} else {
						contentText = WeatherNotificationService.this
								.getString(R.string.press_for_update);
						tickerIcon = android.R.drawable.stat_notify_error;
					}
					setShortAlarm();
				}
			}

			final long when = System.currentTimeMillis();
			// Make notification
			final Notification notification = new Notification(tickerIcon,
					tickerText, when);
			notification.flags = Notification.FLAG_ONGOING_EVENT;
			final Intent notificationIntent = new Intent(
					WeatherNotificationService.this,
					WeatherNotificationService.class);
			final PendingIntent contentIntent = PendingIntent.getService(
					WeatherNotificationService.this, 0, notificationIntent, 0);

			RemoteViews contentView = new RemoteViews(getPackageName(),
					R.layout.weathernotification);
			contentView.setImageViewResource(R.id.icon, contentIcon);
			contentView.setTextViewText(R.id.title, contentTitle);
			contentView.setTextViewText(R.id.text, contentText);
			contentView.setTextViewText(R.id.title, contentTitle);
			contentView.setTextViewText(R.id.time, contentTime);
			// contentView.setTextViewText(R.id.text,
			// "Hello, this message is in a custom expanded view");
			notification.contentView = contentView;
			notification.contentIntent = contentIntent;
			final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(1, notification);

			WeatherNotificationService.this.stopSelf();
		}
	}

	private static final String LOG_ID = "no.firestorm.weatherservice";
	public static final String INTENT_EXTRA_ACTION = "action";
	public static final int INTENT_EXTRA_ACTION_GET_TEMP = 1;

	public static final int INTENT_EXTRA_ACTION_UPDATE_ALARM = 2;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		switch (intent.getIntExtra(INTENT_EXTRA_ACTION,
				INTENT_EXTRA_ACTION_GET_TEMP)) {
		case INTENT_EXTRA_ACTION_GET_TEMP:
			showTemp();
			Log.d(LOG_ID, "show temp");
			break;
		case INTENT_EXTRA_ACTION_UPDATE_ALARM:
			updateAlarm();
			break;

		default:
			break;
		}
		return START_STICKY;
	}

	private void removeAlarm() {
		final PendingIntent pendingIntent = PendingIntent.getService(this, 0,
				new Intent(this, WeatherNotificationService.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		final AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pendingIntent);
	}

	private void setAlarm(long updateRate) {
		// Set alarm
		final PendingIntent pendingIntent = PendingIntent.getService(this, 0,
				new Intent(this, WeatherNotificationService.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		final AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		// Set time to next hour plus 5 minutes:
		final long now = System.currentTimeMillis();
		long triggerAtTime = now;
		// set back to last hour plus 2 minutes:
		triggerAtTime -= triggerAtTime % 3600000 - 12000;
		// Add selected update rate
		triggerAtTime += updateRate * 60000;

		// Set trigger time now earlier than now.
		if (triggerAtTime < now)
			triggerAtTime = now + updateRate * 60000;

		alarm.set(AlarmManager.RTC, triggerAtTime, pendingIntent);
	}

	private void setShortAlarm() {
		setAlarm(10);
	}

	private void showTemp() {
		final ShowTempAsync tempTask = new ShowTempAsync();
		tempTask.execute();

	}

	private void updateAlarm() {
		// Find update rate

		final int updateRate = WsKlimaProxy.getUpdateRate(this);

		if (updateRate <= 0) {
			removeAlarm();
			return;
		} else
			setAlarm(updateRate);
	}

}
