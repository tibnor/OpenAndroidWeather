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
import android.util.Log;

public class WsKlimaProxy {
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
	static final boolean PREFS_USE_NEAREST_STATION_DEFAULT = false;
	// @SuppressWarnings("unused")
	private static final String LOG_ID = "no.weather.weatherProxy.wsKlima.WsKlimaProxy";
	public static final String PREFS_UPDATE_RATE_KEY = "update_rate";
	public static final int PREFS_UPDATE_RATE_DEFAULT = 60;
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
			if (weatherElement.getFrom().getTime() > latestTime) {
				latestTime = weatherElement.getFrom().getTime();
				result = weatherElement;
			}
		return result;
	}

	public static String getLastTemperature(Context context) {
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

	public static boolean getUseNearestStation(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		return settings.getBoolean(PREFS_USE_NEAREST_STATION_KEY,
				PREFS_USE_NEAREST_STATION_DEFAULT);

	}

	public static Date getLastUpdateTime(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		final long result = settings.getLong(PREFS_LAST_UPDATE_TIME_KEY, 0l);
		if (result == 0l)
			return null;
		else
			return new Date(result);
	}

	public static int getStationId(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		return settings.getInt(WsKlimaProxy.PREFS_STATION_ID_KEY,
				WsKlimaProxy.PREFS_STATION_ID_DEFAULT);
	}

	public static String getStationName(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		return settings.getString(WsKlimaProxy.PREFS_STATION_NAME_KEY,
				WsKlimaProxy.PREFS_STATION_NAME_DEFAULT);
	}

	public static int getUpdateRate(Context context) {
		final SharedPreferences settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		return settings
				.getInt(PREFS_UPDATE_RATE_KEY, PREFS_UPDATE_RATE_DEFAULT);
	}

	public static void setStationName(Context context, String name, long id) {
		SharedPreferences preferences = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		final Editor settings = preferences.edit();
		settings.putInt(WsKlimaProxy.PREFS_STATION_ID_KEY, (int) id);
		settings.putString(WsKlimaProxy.PREFS_STATION_NAME_KEY, name);
		settings.remove(PREFS_LAST_UPDATE_TIME_KEY);
		settings.remove(PREFS_LAST_WEATHER_KEY);
		settings.commit();
	}
	
	public static void setUseNearestStation(Context context, boolean useNearestStation) {
		final Editor settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0).edit();
		settings.putBoolean(WsKlimaProxy.PREFS_USE_NEAREST_STATION_KEY, useNearestStation);
		settings.commit();
	}

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

	public WeatherElement getTemperatureNow(Context context)
			throws NetworkErrorException, HttpException {
		try {
			final WeatherElement result = getTemperatureNow(getStationId(context));

			// Save if data
			if (result != null)
				setLastTemperature(context, result.getValue(), result.getFrom());

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
		Log.v(LOG_ID, "url: " + url.toString());

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
				return new WeatherElement(time, time, WeatherType.temperature,
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

	private void setLastTemperature(Context context, String value, Date time) {
		final Editor settings = context.getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0).edit();
		settings.putLong(WsKlimaProxy.PREFS_LAST_UPDATE_TIME_KEY,
				time.getTime());
		settings.putString(PREFS_LAST_WEATHER_KEY, value);
		settings.commit();
	}

}
