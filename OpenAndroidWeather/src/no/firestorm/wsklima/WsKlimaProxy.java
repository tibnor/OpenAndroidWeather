package no.firestorm.wsklima;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import no.firestorm.weathernotificatonservice.WeatherNotificationService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/** Proxy for getting weather measurements from met.no and storing relevant info.
 * 
 * Gets weather measurements indirectly from http://eklima.met.no/wsKlima/ via a 
 * proxy on http://wsklimaproxy.appspot.com/, this to minimize the data downloaded to
 * the device. If the USE_NEAREST_STATION is set to true, the proxy will first check the 
 * position of the device, find the nearest station and then download weather from it.
 * 
 * It's also storing some relevant data:
 *  - Last measurement and the time it was measured
 *  - Selected station
 *  - If user want to get weather from the nearest station
 *   
 */
public class WsKlimaProxy {
	/**
	 * The name of the provider
	 */
	public final static String PROVIDER = "Meteorologisk institutt";
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
	/** Station id to use if the user wants to get the nearest station and not a specific station */
	public static final int FIND_NEAREST_STATION = -100;

	/**
	 * @param weather
	 *            List of weather
	 * @return Latest weather element sorted by From time in WeatherElement
	 */
	static WeatherElement findLatestWeather(List<WeatherElement> weather) {
		long latestTime = 0l;
		WeatherElement result = null;
		for (final WeatherElement weatherElement : weather)
			if (weatherElement.getTime() > latestTime) {
				latestTime = weatherElement.getDate().getTime();
				result = weatherElement;
			}
		return result;
	}

	/** Get saved last temperature. If there has been any successfully downloads
	 * of temperature since the station was set, it returns the last measurement of 
	 * temperature. It does not download any new temperature.
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
	 *  Check if the user want measurements from the nearest station
	 * @param context
	 * @return true if the user want measurements from the nearest station
	 */
	public static boolean isUsingNearestStation(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		return settings.getBoolean(PREFS_USE_NEAREST_STATION_KEY,
				PREFS_USE_NEAREST_STATION_DEFAULT);

	}

	/** Gets the date when the last downloaded measurement was measured.
	 * @param context
	 * @return time for last measurement
	 */
	public static Date getLastUpdateTime(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		final long result = settings.getLong(PREFS_LAST_UPDATE_TIME_KEY, 0l);
		if (result == 0l)
			return null;
		else
			return new Date(result);
	}

	/** Gets the station id for where the measurement is taken. If 
	 * the user want the nearest station this return the last used station.
	 * @param context
	 * @return station id
	 */
	public static int getStationId(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		return settings.getInt(WsKlimaProxy.PREFS_STATION_ID_KEY,
				WsKlimaProxy.PREFS_STATION_ID_DEFAULT);
	}

	/** Gets the station name for where the measurement is taken. If 
	 * the user want the nearest station this return the last used station.
	 * @param context
	 * @return station name
	 */
	public static String getStationName(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		return settings.getString(WsKlimaProxy.PREFS_STATION_NAME_KEY,
				WsKlimaProxy.PREFS_STATION_NAME_DEFAULT);
	}

	/** Gets how often the user wants to update the notification, the alarm is
	 * handeled in WeatherNotificationService.
	 * TODO: move to WeatherNotificationService
	 * 
	 * @param context
	 * @return interval in minutes between each update
	 */
	public static int getUpdateRate(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		return settings
				.getInt(PREFS_UPDATE_RATE_KEY, PREFS_UPDATE_RATE_DEFAULT);
	}

	/** Set the station to be used for updating measurement and delete saved measurement.
	 * If the id is the same as before, nothing is done.
	 * NOTE: {@link WsKlimaProxy#setUseNearestStation(Context, boolean)} must also be set. 
	 * 
	 * 
	 * @param context
	 * @param name
	 * @param id
	 */
	public static void setStation(Context context, String name, int id) {
		SharedPreferences preferences = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		
		// Do nothing if ids are equal
		int oldId = preferences.getInt(PREFS_STATION_ID_KEY, PREFS_STATION_ID_DEFAULT);
		if (oldId == id)
			return;
		
		final Editor settings = preferences.edit();
		settings.putInt(WsKlimaProxy.PREFS_STATION_ID_KEY, (int) id);
		settings.putString(WsKlimaProxy.PREFS_STATION_NAME_KEY, name);
		settings.remove(PREFS_LAST_UPDATE_TIME_KEY);
		settings.remove(PREFS_LAST_WEATHER_KEY);
		settings.commit();
	}
	
	/** Set if the user wants to use the nearest station when updating measurement.
	 * @param context
	 * @param useNearestStation
	 */
	public static void setUseNearestStation(Context context, boolean useNearestStation) {
		final Editor settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0).edit();
		settings.putBoolean(WsKlimaProxy.PREFS_USE_NEAREST_STATION_KEY, useNearestStation);
		settings.commit();
	}

	/** Set how often {@link WeatherNotificationService} should update the notification
	 * @param context
	 * @param updateRate in minutes
	 */
	public static void setUpdateRate(Context context, int updateRate) {
		final Editor settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0).edit();
		settings.putInt(WsKlimaProxy.PREFS_UPDATE_RATE_KEY, updateRate);
		settings.commit();
		updateAlarm(context);
	}

	private static void updateAlarm(Context context) {
		final Intent intent = new Intent(context,
				WeatherNotificationService.class);
		intent.putExtra(WeatherNotificationService.INTENT_EXTRA_ACTION,
				WeatherNotificationService.INTENT_EXTRA_ACTION_UPDATE_ALARM);
		context.startService(intent);
	}

	/** Gets the latest temperature from selected station.
	 * 
	 * NOTE: If user wants to use the nearest station, it must be found first and updated
	 * in {@link #setStation(Context, String, int)}
	 * This is not done automatically!
	 * 
	 * TODO: check if the user wants to use the nearest station
	 * 
	 * @param context
	 * @return the latest temperature
	 * @throws NetworkErrorException
	 * @throws HttpException
	 */
	public WeatherElement getTemperatureNow(Context context)
			throws NetworkErrorException, HttpException {
		try {
			final WeatherElement result = getTemperatureNow(getStationId(context));

			// Save if data
			if (result != null)
				setLastTemperature(context, result.getValue(), result.getDate());

			return result;
		} catch (final NetworkErrorException e) {
			throw e;
		} catch (final HttpException e) {
			throw e;
		}
	}

	WeatherElement getTemperatureNow(Integer station) throws HttpException,
			NetworkErrorException {
		URI url;
		try {
			url = new URI("http://wsklimaproxy.appspot.com/temperature?st="
					+ station);
		} catch (final URISyntaxException e) {
			// Shuld not happend
			e.printStackTrace();
			return null;
		}
		//Log.v(LOG_ID, "url: " + url.toString());

		final HttpClient client = new DefaultHttpClient();
		final HttpGet request = new HttpGet(url);

		try {
			final HttpResponse response = client.execute(request);
			int status = response.getStatusLine().getStatusCode();
			final HttpEntity r_entity = response.getEntity();
			if (status == 200) {
				final String xmlString = EntityUtils.toString(r_entity);
				final JSONObject val = new JSONObject(xmlString);
				final Date time = new Date(val.getLong("time") * 1000);
				return new WeatherElement(time, WeatherType.temperature,
						val.getString("temperature"));
			} else if (status == 204) {
				if (r_entity == null)
					return null;
				else
					throw new HttpException();
			} else {
				throw new NetworkErrorException();
			}
		} catch (final IOException e) {
			throw new NetworkErrorException(e);
		} catch (final JSONException e) {
			throw new HttpException();
		}
	}

	/** Save the last measurements
	 * @param context
	 * @param value
	 * @param time
	 */
	private void setLastTemperature(Context context, String value, Date time) {
		final Editor settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0).edit();
		settings.putLong(WsKlimaProxy.PREFS_LAST_UPDATE_TIME_KEY,
				time.getTime());
		settings.putString(PREFS_LAST_WEATHER_KEY, value);
		settings.commit();
	}

}
