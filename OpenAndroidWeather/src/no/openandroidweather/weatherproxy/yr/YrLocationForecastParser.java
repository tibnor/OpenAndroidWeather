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

import java.util.ArrayList;
import java.util.List;

import no.openandroidweather.misc.IProgressItem;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weathercontentprovider.WeatherType;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.Forecast;
import no.openandroidweather.weatherproxy.YrProxy;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.text.format.Time;

public class YrLocationForecastParser extends DefaultHandler {
	private static final String TAG = "YrLocationForecastParser";
	public final static String NO_NEW_DATA_EXCEPTION = "no new data!";

	ContentResolver contentResolver;
	Uri contentUri;
	boolean isInMeta = false;
	boolean isInTime = false;
	boolean locationIsSet = false;
	long from;
	long to;
	long nextForecastTime = Long.MAX_VALUE;
	long forecastGenerated = 0;
	final long lastGenerated;
	List<ContentValues> values = new ArrayList<ContentValues>();
	IProgressItem progressItem;

	public YrLocationForecastParser(final ContentResolver contentResolver,
			final long lastGeneratedForecastTime, IProgressItem progressItem) {
		super();
		lastGenerated = lastGeneratedForecastTime;
		this.contentResolver = contentResolver;
		this.progressItem = progressItem;
	}

	/**
	 * @param attributes
	 * @param key
	 * @return time in ms since 01.01.1970
	 */
	private static long getTime(final Attributes attributes, final String key) {
		final String timeS = attributes.getValue(attributes.getIndex(key));
		final Time time = new Time("UTC");
		time.parse3339(timeS);

		return time.toMillis(false);
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		progressItem.progress(500 + YrProxy.AFTER_DOWNLOAD_PROGRESS / 2);
		ContentValues valueArray[] = new ContentValues[1];
		valueArray = values.toArray(valueArray);
		contentResolver.bulkInsert(Uri.withAppendedPath(contentUri,
				WeatherContentProvider.Forecast.CONTENT_DIRECTORY), valueArray);
	}

	@Override
	public void endElement(final String uri, final String localName,
			final String qName) throws SAXException {
		if (localName.equals("time"))
			isInTime = false;
		else if (localName.equals("meta"))
			isInMeta = false;
	}

	/**
	 * @return Uri to the content in WeatherContentProvider or null if it only
	 *         updates the expected next forecast time
	 */
	public Uri getContentUri() {
		return contentUri;
	}

	public long getNextExcpectedTime() {
		return nextForecastTime;
	}

	private void insertValue(final int type, final String value) {
		final ContentValues v = new ContentValues();
		v.put(WeatherContentProvider.Forecast.FROM, from);
		v.put(WeatherContentProvider.Forecast.TO, to);
		v.put(WeatherContentProvider.Forecast.TYPE, type);
		v.put(WeatherContentProvider.Forecast.VALUE, value);
		values.add(v);
	}

	private void parseLocation(final Attributes attributes) throws SAXException {

		final double longtitude = new Double(attributes.getValue(attributes
				.getIndex("longitude")));
		final double latitude = new Double(attributes.getValue(attributes
				.getIndex("latitude")));
		final double altitude = new Double(attributes.getValue(attributes
				.getIndex("altitude")));

		if (lastGenerated >= forecastGenerated) {
			final ContentValues values = new ContentValues();
			values.put(WeatherContentProvider.Meta.NEXT_FORECAST,
					nextForecastTime);
			values.put(WeatherContentProvider.Meta.LOADED,
					System.currentTimeMillis());
			final String where = WeatherContentProvider.Meta.PROVIDER + "='"
					+ YrProxy.PROVIDER + "'";
			contentResolver.update(WeatherContentProvider.CONTENT_URI, values,
					where, null);
			contentUri = null;
			// Stops parsing:
			fatalError(new SAXParseException(NO_NEW_DATA_EXCEPTION, null));
		} else {
			final ContentValues values = new ContentValues();
			values.put(WeatherContentProvider.Meta.ALTITUDE, altitude);
			values.put(WeatherContentProvider.Meta.GENERATED, forecastGenerated);
			values.put(WeatherContentProvider.Meta.LATITUDE, latitude);
			values.put(WeatherContentProvider.Meta.LONGITUDE, longtitude);
			values.put(WeatherContentProvider.Meta.NEXT_FORECAST,
					nextForecastTime);
			values.put(WeatherContentProvider.Meta.LOADED,
					System.currentTimeMillis());
			values.put(WeatherContentProvider.Meta.PROVIDER, YrProxy.PROVIDER);
			contentUri = contentResolver.insert(
					WeatherContentProvider.CONTENT_URI, values);
			locationIsSet = true;
		}
	}

	private void parseModel(final Attributes attributes) {
		final long runendedI = getTime(attributes, "runended");
		if (runendedI > forecastGenerated)
			forecastGenerated = runendedI;

		final long nextRunI = getTime(attributes, "nextrun");
		if (nextRunI < nextForecastTime)
			nextForecastTime = nextRunI;
	}

	private void parsePrecipitation(final Attributes attributes) {
		final String value = attributes.getValue(attributes.getIndex("value"));
		insertValue(WeatherType.precipitation, value);
	}

	private void parseSymbol(final Attributes attributes) {
		final String value = attributes.getValue(attributes.getIndex("number"));
		insertValue(WeatherType.symbol, value);
	}

	private void parseTemperature(final Attributes attributes) {
		final String value = attributes.getValue(attributes.getIndex("value"));
		insertValue(WeatherType.temperature, value);
	}

	private void parseTime(final Attributes attributes) {
		isInTime = true;
		from = getTime(attributes, "from");
		to = getTime(attributes, "to");

	}

	private void parseWindDirection(final Attributes attributes) {
		final String value = attributes.getValue(attributes.getIndex("deg"));
		insertValue(WeatherType.windDirection, value);
	}

	private void parseWindSpeed(final Attributes attributes) {
		final String value = attributes.getValue(attributes.getIndex("mps"));
		insertValue(WeatherType.windSpeed, value);
	}

	@Override
	public void startElement(final String uri, final String localName,
			final String qName, final Attributes attributes)
			throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		if (localName.equals("time"))
			parseTime(attributes);
		else if (localName.equals("symbol"))
			parseSymbol(attributes);
		else if (localName.equals("precipitation"))
			parsePrecipitation(attributes);
		else if (localName.equals("temperature"))
			parseTemperature(attributes);
		else if (localName.equals("windSpeed"))
			parseWindSpeed(attributes);
		else if (localName.equals("windDirection"))
			parseWindDirection(attributes);
		else if (localName.equals("meta"))
			isInMeta = true;
		else if (!locationIsSet && localName.equals("location"))
			parseLocation(attributes);
		else if (isInMeta && localName.equals("model"))
			parseModel(attributes);
	}

}
