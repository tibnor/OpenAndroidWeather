/*
	Copyright 2010 Torstein Ingebrigtsen Bø

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

package no.openandroidweather.weatherproxy.yr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weathercontentprovider.WeatherType;
import no.openandroidweather.weatherproxy.YrProxy;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

public class YrLocationForecastParser extends DefaultHandler {
	private static final String TAG = "YrLocationForecastParser";
	public final static String NO_NEW_DATA_EXCEPTION = "no new data!";
	final private static SimpleDateFormat timeFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'hh:mm:ss'Z'");
	ContentResolver contentResolver;
	Uri contentUri;
	boolean isInMeta = false;
	boolean isInTime = false;
	boolean locationIsSet = false;
	long from;
	long to;
	long nextForecastTime = 0;
	long forecastGenerated = 0;
	final long lastGenerated;
	List<ContentValues> values = new ArrayList<ContentValues>();

	public YrLocationForecastParser(ContentResolver contentResolver,
			long lastGeneratedForecastTime) {
		super();
		this.lastGenerated = lastGeneratedForecastTime;
		this.contentResolver = contentResolver;
	}

	/**
	 * @return Uri to the content in WeatherContentProvider or null if it only
	 *         updates the expected next forecast time
	 */
	public Uri getContentUri() {
		return contentUri;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		if (localName.equals("time"))
			parseTime(attributes);
		else if (localName.equals("symbol"))
			parseSymbol(attributes);
		else if (localName.equals("precipitation"))
			parsePrecipitation(attributes);
		else if (localName.equals("temperature"))
			parseTemperature(attributes);
		else if (localName.equals("meta"))
			isInMeta = true;
		else if (!locationIsSet && localName.equals("location"))
			parseLocation(attributes);
		else if (isInMeta && localName.equals("model"))
			parseModel(attributes);
	}

	private void parseTime(Attributes attributes) {
		isInTime = true;
		from = getTime(attributes, "from");
		to = getTime(attributes, "to");
	}

	private void parseModel(Attributes attributes) {
		long runendedI = getTime(attributes, "runended");
		if (runendedI > forecastGenerated)
			forecastGenerated = runendedI;

		long nextRunI = getTime(attributes, "nextrun");
		if (nextRunI > nextForecastTime)
			nextForecastTime = nextRunI;
	}

	/**
	 * @param attributes
	 * @param key
	 * @return time in ms since 01.01.1970
	 */
	private static long getTime(Attributes attributes, String key) {
		String nextrunS = attributes.getValue(attributes.getIndex(key));

		long nextRunI = 0;
		try {
			nextRunI = timeFormat.parse(nextrunS).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return nextRunI;
	}

	private void parseLocation(Attributes attributes) throws SAXException {

		double longtitude = new Double(attributes.getValue(attributes
				.getIndex("longitude")));
		double latitude = new Double(attributes.getValue(attributes
				.getIndex("latitude")));
		double altitude = new Double(attributes.getValue(attributes
				.getIndex("altitude")));

		if (lastGenerated >= forecastGenerated) {
			ContentValues values = new ContentValues();
			values.put(WeatherContentProvider.META_NEXT_FORECAST,
					nextForecastTime);
			String where = WeatherContentProvider.META_PROVIDER + "='"
					+ YrProxy.PROVIDER + "'";
			contentResolver.update(WeatherContentProvider.CONTENT_URI, values,
					where, null);
			contentUri = null;
			// Stops parsing:
			fatalError(new SAXParseException(NO_NEW_DATA_EXCEPTION, null));
		} else {
			ContentValues values = new ContentValues();
			values.put(WeatherContentProvider.META_ALTITUDE, altitude);
			values.put(WeatherContentProvider.META_GENERATED, forecastGenerated);
			values.put(WeatherContentProvider.META_LATITUDE, latitude);
			values.put(WeatherContentProvider.META_LONGITUDE, longtitude);
			values.put(WeatherContentProvider.META_NEXT_FORECAST,
					nextForecastTime);
			values.put(WeatherContentProvider.META_PROVIDER, YrProxy.PROVIDER);
			contentUri = contentResolver.insert(
					WeatherContentProvider.CONTENT_URI, values);
			locationIsSet = true;
		}
	}

	private void parseTemperature(Attributes attributes) {
		String value = attributes.getValue(attributes.getIndex("value"));
		insertValue(WeatherType.temperature, value);
	}

	private void parsePrecipitation(Attributes attributes) {
		String value = attributes.getValue(attributes.getIndex("value"));
		insertValue(WeatherType.precipitation, value);
	}

	private void parseSymbol(Attributes attributes) {
		String value = attributes.getValue(attributes.getIndex("number"));
		insertValue(WeatherType.symbol, value);
	}

	private void insertValue(int type, String value) {
		ContentValues v = new ContentValues();
		v.put(WeatherContentProvider.FORECAST_FROM, from);
		v.put(WeatherContentProvider.FORECAST_TO, to);
		v.put(WeatherContentProvider.FORECAST_TYPE, type);
		v.put(WeatherContentProvider.FORECAST_VALUE, value);
		values.add(v);

	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (localName.equals("time"))
			isInTime = false;
		else if (localName.equals("meta"))
			isInMeta = false;
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		ContentValues valueArray[] = new ContentValues[1];
		valueArray = values.toArray(valueArray);
		contentResolver.bulkInsert(Uri.withAppendedPath(contentUri,
				WeatherContentProvider.FORECAST_CONTENT_DIRECTORY), valueArray);
	}
}
