package no.firestorm.wsklima;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

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
	public static final int PREFS_UPDATE_RATE_DEFAULT = 0;

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

	/**
	 * Make a string with hours for use in getWeather
	 * 
	 * @param from
	 *            earliest time
	 * @param to
	 *            latest time
	 * @return hours between and including from and to, not more than 24
	 */
	static String hours(long from, long to) {
		final SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
		hourFormat.setTimeZone(TimeZone.getTimeZone("gmt"));

		// round to up to nearest hour
		to += (3600000 - to % 3600000);
		// round from down to nearest hour
		from -= from % 3600000;
		String result = hourFormat.format(from);
		from += 3600000;
		int hoursRendered = 1;
		while (from < to && hoursRendered < 24) {
			result += "," + hourFormat.format(from);
			from += 3600000;
			hoursRendered++;
		}
		return result;
	}

	public static <T> String list2csl(List<T> l) {
		if (l == null)
			return "";

		String str = l.get(0).toString();
		for (int i = 1; i < l.size(); i++)
			str += "," + l.get(i).toString();
		return str;
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

	/**
	 * @param station
	 *            integer for station
	 * @param maxAgeInSeconds
	 *            Max age of the data in seconds or maxAgeInSeconds<=0 for any
	 *            age
	 * @return WeatherElements with latest weather
	 * @throws HttpException
	 *             If received document was wrong, i.e. wifi login page
	 * @throws NetworkErrorException
	 *             If problem connecting to host
	 */
	public WeatherElement getTemperatureNow(Integer station,
			Integer maxAgeInSeconds) throws HttpException,
			NetworkErrorException {
		final SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
		date.setTimeZone(TimeZone.getTimeZone("gmt"));
		final SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
		hourFormat.setTimeZone(TimeZone.getTimeZone("gmt"));
		final long nowMS = java.lang.System.currentTimeMillis();
		final long fromMS = nowMS - maxAgeInSeconds * 1000;
		final Date fromDate = new Date(fromMS);
		final Date toDate = new Date(nowMS);

		final String timeSerietypeId = "2";
		final String from = date.format(fromDate);
		final String to = date.format(toDate);
		final String stations = station + "";
		// Get only temperature
		final String elements = "TA";
		final String hours = hours(fromMS, nowMS);
		final String months = "1,2,3,4,5,6,7,8,9,10,11,12";
		final String username = "";

		final List<WeatherElement> weather = getWeather(timeSerietypeId, from,
				to, stations, elements, hours, months, username);

		// Search for the latest
		return findLatestWeather(weather);

	}

	/**
	 * This function get the weather from eklima.met.no/wsklima
	 * 
	 * @param timeSerietypeId
	 * @param from
	 * @param to
	 * @param stations
	 * @param elements
	 * @param hours
	 * @param months
	 * @param username
	 * @return
	 * @throws HttpException
	 *             If received document was wrong, i.e. wifi login page
	 * @throws NetworkErrorException
	 *             If problem connecting to host
	 */
	List<WeatherElement> getWeather(String timeSerietypeId, String from,
			String to, String stations, String elements, String hours,
			String months, String username) throws HttpException,
			NetworkErrorException {

		List<WeatherElement> returnValue = null;
		try {
			final URI url = new URI(
					"http://eklima.met.no/metdata/MetDataService?invoke=getMetData"
							+ "&timeserietypeID=" + timeSerietypeId
							+ "&format=&from=" + from + "&to=" + to
							+ "&stations=" + stations + "&elements=" + elements
							+ "&hours=" + hours + "&months=" + months
							+ "&username=" + username);
			Log.v(LOG_ID, "url: " + url.toString());

			final HttpClient client = new DefaultHttpClient();
			final HttpGet request = new HttpGet(url);
			final HttpResponse response = client.execute(request);
			final HttpEntity r_entity = response.getEntity();
			final String xmlString = EntityUtils.toString(r_entity);
			final InputSource inStream = new InputSource();
			inStream.setCharacterStream(new StringReader(xmlString));

			final SAXParserFactory spf = SAXParserFactory.newInstance();
			final SAXParser sp = spf.newSAXParser();
			final XMLReader xr = sp.getXMLReader();
			final WeatherElementHandler handler = new WeatherElementHandler();
			xr.setContentHandler(handler);
			xr.parse(inStream);
			returnValue = handler.getAllElements();

		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
		} catch (final SAXException e) {
			throw new HttpException(
					"No known cause but could be an wifi login blocking", e);
		} catch (final IOException e) {
			throw new NetworkErrorException(e);
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		}
		return returnValue;
	}

}
