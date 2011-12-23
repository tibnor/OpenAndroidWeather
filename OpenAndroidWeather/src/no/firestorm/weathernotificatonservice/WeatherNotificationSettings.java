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

import java.util.Date;

import no.firestorm.wsklima.WsKlimaProxy;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class WeatherNotificationSettings {
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
	static final String PREFS_DOWNLOAD_ONLY_ON_WIFI_KEY = "download only on wifi";
	static final boolean PREFS_DOWNLOAD_ONLY_ON_WIFI_DEFAULT = false;
	private static final String PREFS_UPDATE_RATE_KEY = "update_rate";
	private static final int PREFS_UPDATE_RATE_DEFAULT = 60;

	/**
	 * Check if the user only wants to download when on wifi
	 * 
	 * @param context
	 * @return true if the user want measurements from the nearest station
	 */
	public static boolean getDownloadOnlyOnWifi(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				PREFS_NAME, 0);
		return settings.getBoolean(PREFS_DOWNLOAD_ONLY_ON_WIFI_KEY,
				PREFS_DOWNLOAD_ONLY_ON_WIFI_DEFAULT);

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
		final long result = settings.getLong(PREFS_LAST_UPDATE_TIME_KEY, 0l);
		if (result == 0l)
			return null;
		else
			return new Date(result);
	}

	/**
	 * Get saved last temperature. If there has been any successfully downloads
	 * of temperature since the station was set, it returns the last measurement
	 * of temperature. It does not download any new temperature.
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
	 * Gets the station id for where the measurement is taken. If the user want
	 * the nearest station this return the last used station.
	 * 
	 * @param context
	 * @return station id
	 */
	public static int getStationId(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				PREFS_NAME, 0);
		return settings.getInt(PREFS_STATION_ID_KEY, PREFS_STATION_ID_DEFAULT);
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
	 * Gets how often the user wants to update the notification, the alarm is
	 * handeled in WeatherNotificationService. TODO: move to
	 * WeatherNotificationService
	 * 
	 * @param context
	 * @return interval in minutes between each update
	 */
	public static int getUpdateRate(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				PREFS_NAME, 0);
		return settings
				.getInt(PREFS_UPDATE_RATE_KEY, PREFS_UPDATE_RATE_DEFAULT);
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

	public static void setDownloadOnlyOnWifi(Context context, boolean enable) {
		final Editor settings = context.getSharedPreferences(PREFS_NAME, 0)
				.edit();
		settings.putBoolean(PREFS_DOWNLOAD_ONLY_ON_WIFI_KEY, enable);
		settings.commit();
	}

	/**
	 * Save the last measurements
	 * 
	 * @param context
	 * @param value
	 * @param time
	 */
	static void setLastTemperature(Context context, String value, Date time) {
		final Editor settings = context.getSharedPreferences(PREFS_NAME, 0)
				.edit();
		settings.putLong(PREFS_LAST_UPDATE_TIME_KEY, time.getTime());
		settings.putString(PREFS_LAST_WEATHER_KEY, value);
		settings.commit();
	}

	/**
	 * Set the station to be used for updating measurement and delete saved
	 * measurement. If the id is the same as before, nothing is done. NOTE:
	 * {@link WsKlimaProxy#setUseNearestStation(Context, boolean)} must also be
	 * set.
	 * 
	 * 
	 * @param context
	 * @param name
	 * @param id
	 */
	public static void setStation(Context context, String name, int id) {
		final SharedPreferences preferences = context.getSharedPreferences(
				PREFS_NAME, 0);

		// Do nothing if ids are equal
		final int oldId = preferences.getInt(PREFS_STATION_ID_KEY,
				PREFS_STATION_ID_DEFAULT);
		if (oldId == id)
			return;

		final Editor settings = preferences.edit();
		settings.putInt(PREFS_STATION_ID_KEY, id);
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
		settings.putBoolean(PREFS_USE_NEAREST_STATION_KEY, useNearestStation);
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
