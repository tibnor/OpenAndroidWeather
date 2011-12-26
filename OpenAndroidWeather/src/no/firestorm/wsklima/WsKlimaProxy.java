package no.firestorm.wsklima;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import no.firestorm.misc.CheckInternetStatus;
import no.firestorm.weathernotificatonservice.WeatherNotificationSettings;
import no.firestorm.wsklima.database.WsKlimaDataBaseHelper;

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

/**
 * Proxy for getting weather measurements from met.no and storing relevant info.
 * 
 * Gets weather measurements indirectly from http://eklima.met.no/wsKlima/ via a
 * proxy on http://wsklimaproxy.appspot.com/, this to minimize the data
 * downloaded to the device. If the USE_NEAREST_STATION is set to true, the
 * proxy will first check the position of the device, find the nearest station
 * and then download weather from it.
 * 
 * It's also storing some relevant data: - Last measurement and the time it was
 * measured - Selected station - If user want to get weather from the nearest
 * station
 * 
 */
public class WsKlimaProxy {
	/**
	 * The name of the provider
	 */
	public final static String PROVIDER = "Meteorologisk institutt";

	/**
	 * Station id to use if the user wants to get the nearest station and not a
	 * specific station
	 */
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

	/**
	 * Gets the latest temperature from selected station.
	 * 
	 * NOTE: If user wants to use the nearest station, it must be found first
	 * and updated in {@link #setStation(Context, String, int)} This is not done
	 * automatically!
	 * 
	 * 
	 * @param context
	 * @return the latest temperature
	 * @throws NetworkErrorException
	 * @throws HttpException
	 */
	public WeatherElement getTemperatureNow(Integer station, Context context)
			throws HttpException, NetworkErrorException {

		// Check if it's in download on wifi only mode, if so check if wifi is
		// connected,
		// if not throw an error
		if (WeatherNotificationSettings.getDownloadOnlyOnWifi(context))
			if (!CheckInternetStatus.isWifiConnected(context))
				throw new NetworkErrorException();

		URI url;
		try {
			url = new URI("http://wsklimaproxy.appspot.com/temperature?st="
					+ station);
		} catch (final URISyntaxException e) {
			// Should not happen
			e.printStackTrace();
			return null;
		}
		// Log.v(LOG_ID, "url: " + url.toString());

		final HttpClient client = new DefaultHttpClient();
		final HttpGet request = new HttpGet(url);

		try {
			final HttpResponse response = client.execute(request);
			final int status = response.getStatusLine().getStatusCode();
			final HttpEntity r_entity = response.getEntity();
			if (status == 200) {
				final String xmlString = EntityUtils.toString(r_entity);
				final JSONObject val = new JSONObject(xmlString);
				final Date time = new Date(val.getLong("time") * 1000);
				final boolean isReliable = val.getBoolean("reliable");
				// If not reliable save it in db
				if (!isReliable) {
					final WsKlimaDataBaseHelper db = new WsKlimaDataBaseHelper(
							context);
					db.setIsReliable(station, isReliable);
				}
				return new WeatherElement(time, WeatherType.temperature,
						val.getString("temperature"));
			} else if (status == 204) {
				// Currently no data but the station is reliable
				if (r_entity == null) {
					return null;
				} else
					throw new HttpException();
			} else if (status == 410) {
				// Station is not reliable
					final WsKlimaDataBaseHelper db = new WsKlimaDataBaseHelper(
							context);
					db.setIsReliable(station, false);
					return null;

			} else
				throw new NetworkErrorException();
		} catch (final IOException e) {
			throw new NetworkErrorException(e);
		} catch (final JSONException e) {
			throw new HttpException();
		}
	}

}
