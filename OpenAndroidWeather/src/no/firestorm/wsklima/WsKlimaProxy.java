package no.firestorm.wsklima;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

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
import android.util.Log;

public class WsKlimaProxy {
	public final static String PROVIDER = "Meteorologisk institutt";
	public static final String PREFS_NAME = "no.WsKlimaProxy";
	public static final String PREFS_STATION_ID_KEY = "station_id";
	public static final int PREFS_STATION_ID_DEFAULT = 18700;
	public static final String PREFS_STATION_NAME_KEY = "station_name";
	public static final String PREFS_STATION_NAME_DEFAULT = "OSLO - BLINDERN";
	//@SuppressWarnings("unused")
	private static final String LOG_ID = "no.weather.weatherProxy.wsKlima.WsKlimaProxy";
	public static final String PREFS_UPDATE_RATE_KEY = "update_rate";
	public static final int PREFS_UPDATE_RATE_DEFAULT = 60;

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

	public WeatherElement getTemperatureNowSmall(Integer station)
			throws HttpException, NetworkErrorException {
		URI url;
		try {
			url = new URI("http://wsklimaproxy.appspot.com/temperature?st="
					+ station);
		} catch (URISyntaxException e) {
			// Shuld not happend
			e.printStackTrace();
			return null;
		}
		Log.v(LOG_ID, "url: " + url.toString());

		final HttpClient client = new DefaultHttpClient();
		final HttpGet request = new HttpGet(url);

		try {
			HttpResponse response = client.execute(request);
			final HttpEntity r_entity = response.getEntity();
			final String xmlString = EntityUtils.toString(r_entity);
			JSONObject val = new JSONObject(xmlString);
			Date time = new Date(val.getLong("time") * 1000);
			return new WeatherElement(time, time, WeatherType.temperature,
					val.getString("temperature"));
		} catch (IOException e) {
			throw new NetworkErrorException(e);
		} catch (JSONException e) {
			throw new HttpException();
		}
	}

}
