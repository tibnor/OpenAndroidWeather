package no.firestorm.wsklima;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import no.firestorm.misc.CheckInternetStatus;
import no.firestorm.weathernotificatonservice.WeatherNotificationSettings;
import no.firestorm.wsklima.database.WsKlimaDataBaseHelper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.text.format.Time;
import android.util.Log;

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
				latestTime = weatherElement.getTime();
				result = weatherElement;
			}
		return result;
	}

	/**
	 * Gets the latest temperature from selected station.
	 * 
	 * 
	 * @param station
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
				final Time time = new Time();
				time.set(val.getLong("time") * 1000);
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

	public List<WeatherElement> getWeather(Integer station,Integer timeserieType, String elements)
			throws ClientProtocolException, IOException {
		Time now = new Time();
		now.setToNow();
		return getWeather(station,timeserieType, elements, now);
	}
	
	public List<WeatherElement> getWeather(Integer station,
			Integer timeserieType, String elements, Time time)
			throws ClientProtocolException, IOException {
		time.switchTimezone("UTC");
		String hour = Integer.toString(time.hour);
		return getWeather(station, timeserieType, elements, time,hour);
	}

	public List<WeatherElement> getWeather(Integer station,
			Integer timeserieType, String elements, Time time,String hours)
			throws ClientProtocolException, IOException {
		time.switchTimezone("UTC");
		String date = time.year + "-" + (time.month + 1) + "-" + time.monthDay;
		URI url = null;
		try {
			url = new URI(
					"http://eklima.met.no/metdata/MetDataService?invoke=getMetData&timeserietypeID="
							+ timeserieType
							+ "&format=&from="
							+ date
							+ "&to="
							+ date
							+ "&stations="
							+ station
							+ "&elements="
							+ elements
							+ "&hours="
							+ hours
							+ "&months="
							+ (time.month + 1) + "&username=");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		final HttpClient client = new DefaultHttpClient();
		final HttpGet request = new HttpGet(url);
		Log.i("firestorm", url.toString());

		final HttpResponse response = client.execute(request);
		final int status = response.getStatusLine().getStatusCode();
		final HttpEntity r_entity = response.getEntity();
		if (status != 200) {
			throw new IOException();
		}
		final String xmlString = EntityUtils.toString(r_entity);
		Pattern p = Pattern.compile("<value.*?>(.*?)</value>");
		Matcher m = p.matcher(xmlString);
		List<String> values = new LinkedList<String>();
		while (m.find()) {
			values.add(m.group(1));
		}
		if(values.isEmpty())
			return null;
		
		p = Pattern.compile("<id.*?>(.*?)</id>");
		m = p.matcher(xmlString);
		List<String> types = new LinkedList<String>();
		while (m.find()) {
			if (!Pattern.matches("^[0-9]*$", m.group(1)) )
				types.add(m.group(1));
		}
		if(types.isEmpty())
			return null;
		
		p = Pattern.compile("<from.*?>(.*?)</from>");
		m = p.matcher(xmlString);
		if(!m.find())
			return null;
		Time fromT = new Time();
		fromT.parse3339(m.group(1));

		int i = 0;
		List<WeatherElement> result = new ArrayList<WeatherElement>(
				values.size());
		for (String val : values) {
			if (val.contains("-99999"))
				val = "";
			WeatherElement w = new WeatherElement(fromT,
					idToWeatherType(types.get(i)), val);
			i++;
			result.add(w);
		}
		return result;
	}

	private WeatherType idToWeatherType(String id) {
		if (id.equals("TA"))
			return WeatherType.temperature;
		else if (id.equals("TAX"))
			return WeatherType.temperatureMax;
		else if (id.equals("TAN"))
			return WeatherType.temperatureMin;
		else if (id.equals("RR"))
			return WeatherType.precipitation;
		else if (id.equals("RR_1"))
			return WeatherType.precipitationLastHour;
		else if (id.equals("RR_12"))
			return WeatherType.precipitationLast12h;
		else if (id.equals("FF"))
			return WeatherType.windSpeed;
		else if (id.equals("DD"))
			return WeatherType.windDirection;
		else if (id.equals("FG_1"))
			return WeatherType.windGustSpeed;
		else if (id.equals("FXX"))
			return WeatherType.windSpeedMax;
		else if (id.equals("FGX"))
			return WeatherType.windGustSpeedMax;
		else if (id.equals("RA"))
			return WeatherType.precipitationInBucket;
		else
			throw new UnknownError("Uknown type: "+id);
	}
}
