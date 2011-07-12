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
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.widget.RemoteViews;

/**
 * Updates notification if extra intent is INTENT_EXTRA_ACTION_GET_TEMP, for
 * updating the notification or INTENT_EXTRA_ACTION_UPDATE_ALARM for setting the
 * alarm for the next time the notification should be updated.
 * 
 * Start service for updating notification by:
 * 
 * @code final Intent intent = new Intent(this,
 *       WeatherNotificationService.class);
 *       intent.putExtra(WeatherNotificationService.INTENT_EXTRA_ACTION,
 *       WeatherNotificationService.INTENT_EXTRA_ACTION_GET_TEMP);
 *       startService(intent);
 * 
 * @endcode
 */
public class WeatherNotificationService extends IntentService {

	/**
	 * Listen for location updates, when accuracy is within 3000m it will find
	 * the closest station and set it in WsKlimaProxy. Remove itself as a
	 * location listener and set stationReady=true, before it notify the
	 * service.
	 * 
	 */
	private class UpdateStation implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			if (location.getAccuracy() < 3000) {
				Context context = WeatherNotificationService.this;
				WsKlimaDataBaseHelper db = new WsKlimaDataBaseHelper(context);
				Station station = db.getStationsSortedByLocation(location,true).get(
						0);
				WsKlimaProxy.setStation(context, station.getName(),
						station.getId());
				// Remove listener
				removeFromBroadcaster();

				synchronized (WeatherNotificationService.this) {
					WeatherNotificationService.this.setIsUpdateStationCompleted(true);
					WeatherNotificationService.this.notify();
				}
			}

		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void removeFromBroadcaster() {
			Context context = WeatherNotificationService.this;
			LocationManager locManager = (LocationManager) context
					.getSystemService(LOCATION_SERVICE);
			locManager.removeUpdates(this);
		}

	}

	/**
	 * Used to check if the update of station is completed, updateStation set it to true when completed
	 */
	private Boolean isUpdateStationCompleted = true;

	@SuppressWarnings("unused")
	private static final String LOG_ID = "no.firestorm.weatherservice";

	/**
	 * Key for specifying action when starting the service @see
	 * WeatherNotificationService
	 */
	public static final String INTENT_EXTRA_ACTION = "action";

	/**
	 * If INTENT_EXTRA_ACTION parameter in startup intent is set to this, the
	 * service update the notification.
	 */
	public static final int INTENT_EXTRA_ACTION_GET_TEMP = 1;

	/**
	 * If INTENT_EXTRA_ACTION parameter in startup intent is set to this, the
	 * service update the alarm for updating the notification.
	 */
	public static final int INTENT_EXTRA_ACTION_UPDATE_ALARM = 2;

	/**
	 * 
	 */
	public WeatherNotificationService() {
		super("WeatherNotifivationService");
	}

	/**
	 * @param name
	 */
	public WeatherNotificationService(String name) {
		super(name);
	}

	/**
	 * Downloads weather data
	 * 
	 * @return temperature
	 * @throws NetworkErrorException
	 *             if download problem
	 * @throws HttpException
	 *             if download problem
	 * @throws NoLocationException
	 *             if location provider does not give any location to
	 *             UpdateStation in two minutes
	 */
	protected WeatherElement getWeatherData() throws NetworkErrorException,
			HttpException, NoLocationException {

		// Wait for location update
		synchronized (this) {
			if (!isUpdateStationCompleted) {
				try {
					this.wait(2 * 60 * 1000);
				} catch (InterruptedException e) {
					throw new NoLocationException(e);
				}
				if (!isUpdateStationCompleted)
					throw new NoLocationException(null);
			}

		}

		// get temp
		final WsKlimaProxy weatherProxy = new WsKlimaProxy();
		return weatherProxy.getTemperatureNow(WeatherNotificationService.this);
	}

	/**
	 * Shows a notification with information about the error, either NoLocation
	 * or DownloadError (default)
	 * 
	 * @param e
	 *            Exception
	 */
	private void makeNotification(Exception e) {
		int tickerIcon, contentIcon;
		CharSequence tickerText, contentTitle, contentText, contentTime;
		final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
		contentIcon = android.R.drawable.stat_notify_error;
		Context context = WeatherNotificationService.this;
		contentTime = df.format(new Date());

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
					.getSavedLastTemperature(context));
			contentText = String.format("%s %.1f °C %s %s",
					context.getString(R.string.last_temperature), temperatureF,
					context.getString(R.string._tid_), df.format(lastTime));
			tickerIcon = TempToDrawable.getDrawableFromTemp(temperatureF);

		} else {
			contentText = WeatherNotificationService.this
					.getString(R.string.press_for_update);
			tickerIcon = android.R.drawable.stat_notify_error;
		}

		// Set an alarm for a update within a short time
		setShortAlarm();

		makeNotification(tickerIcon, contentIcon, tickerText, contentTitle,
				contentText, contentTime);

	}
	/**
	 * Make notification and post it to the NotificationManager
	 * 
	 * @param tickerIcon
	 *            Icon shown in notification bar
	 * @param contentIcon
	 *            Icon shown in notification
	 * @param tickerText
	 *            Text shown in notification bar
	 * @param contentTitle
	 *            Title shown in notification
	 * @param contentText
	 *            Description shown in notification
	 * @param contentTime
	 *            Time shown in notification
	 */
	private void makeNotification(int tickerIcon, int contentIcon,
			CharSequence tickerText, CharSequence contentTitle,
			CharSequence contentText, CharSequence contentTime) {
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
		notification.contentView = contentView;
		notification.contentIntent = contentIntent;

		// Post notification
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(1, notification);
	}
	/**
	 * Display a notification with temperature
	 * 
	 * @param weather
	 *            Weather element to with data to be shown in notification, if
	 *            null a message for that say that this station does not provide
	 *            data will be shown
	 */
	private void makeNotification(WeatherElement weather) {
		int tickerIcon, contentIcon;
		CharSequence tickerText, contentTitle, contentText, contentTime;
		final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
		if (weather != null) {
			// Has data
			final WeatherElement temperature = (WeatherElement) weather;
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
			contentTime = df.format(temperature.getDate());

			final Context context = WeatherNotificationService.this;
			contentText = String.format("%s %.1f °C", context
					.getString(R.string.temperatur_),
					new Float(temperature.getValue()));
			updateAlarm();

		} else {
			// No data
			contentIcon = android.R.drawable.stat_notify_error;
			Context context = WeatherNotificationService.this;
			contentTime = df.format(new Date());
			tickerText = context.getText(R.string.no_available_data);
			contentTitle = context.getText(R.string.no_available_data);
			contentText = context.getString(R.string.try_another_station);
			tickerIcon = android.R.drawable.stat_notify_error;

		}

		makeNotification(tickerIcon, contentIcon, tickerText, contentTitle,
				contentText, contentTime);

	}
	@Override
	protected void onHandleIntent(Intent intent) {
		switch (intent.getIntExtra(INTENT_EXTRA_ACTION,
				INTENT_EXTRA_ACTION_GET_TEMP)) {
		case INTENT_EXTRA_ACTION_GET_TEMP:
			showTemp();
			break;
		case INTENT_EXTRA_ACTION_UPDATE_ALARM:
			updateAlarm();
			break;
		default:
			showTemp();
			break;
		}
	}

	private void removeAlarm() {
		final PendingIntent pendingIntent = PendingIntent.getService(this, 0,
				new Intent(this, WeatherNotificationService.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		final AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pendingIntent);
	}

	/**
	 * Set alarm
	 * 
	 * @param updateRate
	 *            in minutes
	 */
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

	/**
	 * @param ready If true UpdateStation has completed
	 */
	private void setIsUpdateStationCompleted(boolean ready) {
		isUpdateStationCompleted = ready;
	}

	private void setShortAlarm() {
		setAlarm(10);
	}

	/**
	 * Update the temperature
	 */
	void showTemp() {
		try {
			updateLocation();
			WeatherElement returnValue = getWeatherData();
			makeNotification(returnValue);
		} catch (NetworkErrorException e) {
			makeNotification(e);
		} catch (HttpException e) {
			makeNotification(e);
		} catch (NoLocationException e) {
			makeNotification(e);
		}

	}

	/**
	 * Set alarm for next time the notification should be updated
	 */
	private void updateAlarm() {
		// Find update rate

		final int updateRate = WsKlimaProxy.getUpdateRate(this);

		if (updateRate <= 0) {
			removeAlarm();
			return;
		} else
			setAlarm(updateRate);
	}

	/**
	 * If isUsingNearestStation is false, it doesn't do anything. If true it
	 * register an UpdateStation to listen for location update.
	 * 
	 * @see UpdateStation
	 * 
	 * @throws NoLocationException
	 *             if no provider is available
	 */
	private void updateLocation() throws NoLocationException {
		Context context = this;
		if (WsKlimaProxy.isUsingNearestStation(context)) {
			// find nearest station
			// find current position
			LocationManager locMan = (LocationManager) context
					.getSystemService(LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			criteria.setPowerRequirement(Criteria.POWER_LOW);
			String provider = locMan.getBestProvider(criteria, true);
			UpdateStation updateStation = new UpdateStation();
			if (provider != null) {
				isUpdateStationCompleted = false;
				locMan.requestLocationUpdates(provider, 0, 0, updateStation,
						Looper.getMainLooper());
			} else {
				throw new NoLocationException(null);
			}
		}
	}

}
