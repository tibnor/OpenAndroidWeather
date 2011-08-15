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
import java.util.List;

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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
	 * Listen for location updates, when accuracy is better than the distance
	 * between the two closes stations, it will find the closest station and set
	 * it in WsKlimaProxy. Remove itself as a location listener and set
	 * stationReady=true, before it notify the service.
	 * 
	 */
	private class UpdateStation implements LocationListener {
		// Last location
		Location mLocation = null;
		Float accuracyDemand = Float.MAX_VALUE;

		@Override
		public void onLocationChanged(Location location) {
			mLocation = location;

			// Check if location is within accuracy demand
			if (location.getAccuracy() < accuracyDemand) {
				// update accuracy demand
				Context context = WeatherNotificationService.this;
				WsKlimaDataBaseHelper db = new WsKlimaDataBaseHelper(context);
				List<Station> stations = db.getStationsSortedByLocation(
						location, true);
				accuracyDemand = stations.get(1).getDistanceToCurrentPosition()
						- stations.get(0).getDistanceToCurrentPosition();

				if (location.getAccuracy() < accuracyDemand) {
					try {
						saveStation();
					} catch (NoLocationException e) {
						e.printStackTrace();
					}
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

		public void saveStation() throws NoLocationException {
			if (mLocation != null) {
				Context context = WeatherNotificationService.this;
				WsKlimaDataBaseHelper db = new WsKlimaDataBaseHelper(context);
				List<Station> stations = db.getStationsSortedByLocation(
						mLocation, true);
				Station station = stations.get(0);
				Settings.setStation(context, station.getName(), station.getId());
				// Remove listener
				removeFromBroadcaster();

				synchronized (WeatherNotificationService.this) {
					WeatherNotificationService.this
							.setIsUpdateStationCompleted(true);
					WeatherNotificationService.this.notify();
				}
			} else
				throw new NoLocationException();
		}
	}

	/**
	 * Used to check if the update of station is completed, updateStation set it
	 * to true when completed
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
	 * Downloads weather data If isUsingNearestStation is false, it doesn't do
	 * update station. If true it update the station.
	 * 
	 * @see UpdateStation
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

		Context context = this;
		// Update station
		if (Settings.isUsingNearestStation(context)) {
			// Find location provider
			LocationManager locMan = (LocationManager) context
					.getSystemService(LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			criteria.setPowerRequirement(Criteria.POWER_LOW);
			String provider = locMan.getBestProvider(criteria, true);

			if (provider != null) {
				// Register provider
				isUpdateStationCompleted = false;
				UpdateStation updateStation = new UpdateStation();
				locMan.requestLocationUpdates(provider, 0, 0, updateStation,
						Looper.getMainLooper());

				// Wait for the updating of station to complete
				synchronized (this) {
					if (!isUpdateStationCompleted) {
						try {
							this.wait(2 * 60 * 1000);
						} catch (InterruptedException e) {
							// If it did not get a good enough accuracy within 2
							// minutes, use the latest one
							updateStation.saveStation();
						}
						if (!isUpdateStationCompleted)
							updateStation.saveStation();
					}

				}
			} else {
				throw new NoLocationException(null);
			}
		}

		// get temp
		final WsKlimaProxy weatherProxy = new WsKlimaProxy();
		WeatherElement result = weatherProxy.getTemperatureNow(
				Settings.getStationId(context), context);
		// Save if data
		if (result != null)
			Settings.setLastTemperature(context, result.getValue(),
					result.getDate());
		return result;
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

		Date lastTime = Settings
				.getLastUpdateTime(WeatherNotificationService.this);
		if (lastTime != null) {
			Float temperatureF = Float.parseFloat(Settings
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
			final String stationName = Settings
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
	 * @param ready
	 *            If true UpdateStation has completed
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

		final int updateRate = Settings.getUpdateRate(this);

		if (updateRate <= 0) {
			removeAlarm();
			return;
		} else
			setAlarm(updateRate);
	}

	public static class Settings {
		static final String PREFS_NAME = "no.WsKlimaProxy";
		static final String PREFS_STATION_ID_KEY = "station_id";
		static final int PREFS_STATION_ID_DEFAULT = 18700;
		static final String PREFS_STATION_NAME_KEY = "station_name";
		static final String PREFS_STATION_NAME_DEFAULT = "Oslo - Blindern";
		static final String PREFS_LAST_WEATHER_KEY = "latest_weather";
		static final Integer PREFS_LAST_WEATHER_DEFAULT = null;
		static final String PREFS_LAST_UPDATE_TIME_KEY = "last_update_time";
		static final long PREFS_LAST_UPDATE_TIME_DEFAULT = 0l;
		static final String PREFS_USE_NEAREST_STATION_KEY = "use_nearest_station";
		static final boolean PREFS_USE_NEAREST_STATION_DEFAULT = true;
		private static final String PREFS_UPDATE_RATE_KEY = "update_rate";
		private static final int PREFS_UPDATE_RATE_DEFAULT = 60;

		/**
		 * Save the last measurements
		 * 
		 * @param context
		 * @param value
		 * @param time
		 */
		private static void setLastTemperature(Context context, String value,
				Date time) {
			final Editor settings = context.getSharedPreferences(PREFS_NAME, 0)
					.edit();
			settings.putLong(PREFS_LAST_UPDATE_TIME_KEY, time.getTime());
			settings.putString(PREFS_LAST_WEATHER_KEY, value);
			settings.commit();
		}

		/**
		 * Gets the date when the last downloaded measurement was measured.
		 * 
		 * @param context
		 * @return time for last measurement
		 */
		public static Date getLastUpdateTime(Context context) {
			final SharedPreferences settings = context.getSharedPreferences(
					PREFS_NAME, 0);
			final long result = settings
					.getLong(PREFS_LAST_UPDATE_TIME_KEY, 0l);
			if (result == 0l)
				return null;
			else
				return new Date(result);
		}

		/**
		 * Get saved last temperature. If there has been any successfully
		 * downloads of temperature since the station was set, it returns the
		 * last measurement of temperature. It does not download any new
		 * temperature.
		 * 
		 * @param context
		 * @return last downloaded temperature
		 */
		public static String getSavedLastTemperature(Context context) {
			final SharedPreferences settings = context.getSharedPreferences(
					PREFS_NAME, 0);
			final String key = PREFS_LAST_WEATHER_KEY;
			final String defaultV = "empty";
			final String result = settings.getString(key, defaultV);
			if (result == defaultV)
				return null;
			else
				return result;
		}

		/**
		 * Gets the station id for where the measurement is taken. If the user
		 * want the nearest station this return the last used station.
		 * 
		 * @param context
		 * @return station id
		 */
		public static int getStationId(Context context) {
			final SharedPreferences settings = context.getSharedPreferences(
					PREFS_NAME, 0);
			return settings.getInt(PREFS_STATION_ID_KEY,
					PREFS_STATION_ID_DEFAULT);
		}

		/**
		 * Gets the station name for where the measurement is taken. If the user
		 * want the nearest station this return the last used station.
		 * 
		 * @param context
		 * @return station name
		 */
		public static String getStationName(Context context) {
			final SharedPreferences settings = context.getSharedPreferences(
					PREFS_NAME, 0);
			return settings.getString(PREFS_STATION_NAME_KEY,
					PREFS_STATION_NAME_DEFAULT);
		}

		/**
		 * Gets how often the user wants to update the notification, the alarm
		 * is handeled in WeatherNotificationService. TODO: move to
		 * WeatherNotificationService
		 * 
		 * @param context
		 * @return interval in minutes between each update
		 */
		public static int getUpdateRate(Context context) {
			final SharedPreferences settings = context.getSharedPreferences(
					PREFS_NAME, 0);
			return settings.getInt(PREFS_UPDATE_RATE_KEY,
					PREFS_UPDATE_RATE_DEFAULT);
		}

		/**
		 * Check if the user want measurements from the nearest station
		 * 
		 * @param context
		 * @return true if the user want measurements from the nearest station
		 */
		public static boolean isUsingNearestStation(Context context) {
			final SharedPreferences settings = context.getSharedPreferences(
					PREFS_NAME, 0);
			return settings.getBoolean(PREFS_USE_NEAREST_STATION_KEY,
					PREFS_USE_NEAREST_STATION_DEFAULT);

		}

		/**
		 * Set the station to be used for updating measurement and delete saved
		 * measurement. If the id is the same as before, nothing is done. NOTE:
		 * {@link WsKlimaProxy#setUseNearestStation(Context, boolean)} must also
		 * be set.
		 * 
		 * 
		 * @param context
		 * @param name
		 * @param id
		 */
		public static void setStation(Context context, String name, int id) {
			SharedPreferences preferences = context.getSharedPreferences(
					PREFS_NAME, 0);

			// Do nothing if ids are equal
			int oldId = preferences.getInt(PREFS_STATION_ID_KEY,
					PREFS_STATION_ID_DEFAULT);
			if (oldId == id)
				return;

			final Editor settings = preferences.edit();
			settings.putInt(PREFS_STATION_ID_KEY, (int) id);
			settings.putString(PREFS_STATION_NAME_KEY, name);
			settings.remove(PREFS_LAST_UPDATE_TIME_KEY);
			settings.remove(PREFS_LAST_WEATHER_KEY);
			settings.commit();
		}

		/**
		 * Set how often {@link WeatherNotificationService} should update the
		 * notification
		 * 
		 * @param context
		 * @param updateRate
		 *            in minutes
		 */
		public static void setUpdateRate(Context context, int updateRate) {
			final Editor settings = context.getSharedPreferences(PREFS_NAME, 0)
					.edit();
			settings.putInt(PREFS_UPDATE_RATE_KEY, updateRate);
			settings.commit();
			updateAlarm(context);
		}

		/**
		 * Set if the user wants to use the nearest station when updating
		 * measurement.
		 * 
		 * @param context
		 * @param useNearestStation
		 */
		public static void setUseNearestStation(Context context,
				boolean useNearestStation) {
			final Editor settings = context.getSharedPreferences(PREFS_NAME, 0)
					.edit();
			settings.putBoolean(PREFS_USE_NEAREST_STATION_KEY,
					useNearestStation);
			settings.commit();
		}

		private static void updateAlarm(Context context) {
			final Intent intent = new Intent(context,
					WeatherNotificationService.class);
			intent.putExtra(WeatherNotificationService.INTENT_EXTRA_ACTION,
					WeatherNotificationService.INTENT_EXTRA_ACTION_UPDATE_ALARM);
			context.startService(intent);
		}

	}

}
