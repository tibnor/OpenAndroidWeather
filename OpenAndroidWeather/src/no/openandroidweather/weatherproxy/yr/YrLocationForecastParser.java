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
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.ForecastListView;
import no.openandroidweather.weathercontentprovider.WeatherType;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.text.format.Time;

public class YrLocationForecastParser extends DefaultHandler {
	@SuppressWarnings("unused")
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
	List<ContentValues> rawValues = new ArrayList<ContentValues>();
	List<ContentValues> forecastListValues = new ArrayList<ContentValues>();
	IProgressItem progressItem;
	private final static Double TEMPERATURE_NOT_SET_VALUE = -300.;
	private Double mTemperature = TEMPERATURE_NOT_SET_VALUE;
	private Double mWindDirection = -1.;
	private Double mWindSpeed = -1.;
	private Integer mSymbol = -1;
	private Double mPercipitation = -1.;
	private boolean hasParsedForecastRow = false;

	public YrLocationForecastParser(final ContentResolver contentResolver,
			final long lastGeneratedForecastTime,
			final IProgressItem progressItem) {
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

	/**
	 * Check if all data is parsed for a forecastList row, if so it is saved in
	 * the database and hasParsedForecastRow is set to true
	 */
	private void checkForecastRow() {
		if (!hasParsedForecastRow && mTemperature != TEMPERATURE_NOT_SET_VALUE
				&& mWindDirection >= 0 && mWindSpeed >= 0 && mSymbol >= 0
				&& mPercipitation >= 0) {
			// Adds values
			ContentValues values = new ContentValues();
			values.put(ForecastListView.fromTime, from);
			values.put(ForecastListView.percipitation, mPercipitation);
			values.put(ForecastListView.symbol, mSymbol);
			values.put(ForecastListView.toTime, to);
			values.put(ForecastListView.temperature, mTemperature);
			values.put(ForecastListView.windDirection, mWindDirection);
			values.put(ForecastListView.windSpeed, mWindSpeed);
			forecastListValues.add(values);
			hasParsedForecastRow = true;

			resetCached();
		}

	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		// Post progress
		progressItem.progress(500);
		// Stores raw values
		ContentValues[] values = new ContentValues[0];
		values = rawValues.toArray(values);
		contentResolver.bulkInsert(Uri.withAppendedPath(contentUri,
				WeatherContentProvider.Forecast.CONTENT_DIRECTORY),
				values);

		// Post progress
		progressItem.progress(750);
		// Stores values for forecast list view
		values = new ContentValues[0];
		values = forecastListValues.toArray(values);
		contentResolver
				.bulkInsert(Uri.withAppendedPath(contentUri,
						ForecastListView.CONTENT_PATH),
						values);

	}

	@Override
	public void endElement(final String uri, final String localName,
			final String qName) throws SAXException {
		if (localName.equals("time")) {
			isInTime = false;
			checkForecastRow();
		} else if (localName.equals("meta"))
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
		rawValues.add(v);
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

	/**
	 * Parse model attributes get next forecast time and forecast generated
	 * 
	 * @param attributes
	 */
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
		mPercipitation = new Double(value);
	}

	private void parseSymbol(final Attributes attributes) {
		final String value = attributes.getValue(attributes.getIndex("number"));
		insertValue(WeatherType.symbol, value);
		mSymbol = new Integer(value);
	}

	private void parseTemperature(final Attributes attributes) {
		final String value = attributes.getValue(attributes.getIndex("value"));
		insertValue(WeatherType.temperature, value);
		mTemperature = new Double(value);
	}

	private void parseTime(final Attributes attributes) {
		isInTime = true;
		long newFrom = getTime(attributes, "from");
		to = getTime(attributes, "to");

		// If the forecast is a new starting time, reset hasParsedForecastRow
		if (newFrom != from) {
			hasParsedForecastRow = false;
			from = newFrom;
			resetCached();
		}

	}

	private void parseWindDirection(final Attributes attributes) {
		final String value = attributes.getValue(attributes.getIndex("deg"));
		insertValue(WeatherType.windDirection, value);
		mWindDirection = new Double(value);
	}

	private void parseWindSpeed(final Attributes attributes) {
		final String value = attributes.getValue(attributes.getIndex("mps"));
		insertValue(WeatherType.windSpeed, value);
		mWindSpeed = new Double(value);
	}

	/**
	 * Reset data cached for forecast list view table
	 */
	private void resetCached() {
		// Resets values:
		mTemperature = TEMPERATURE_NOT_SET_VALUE;
		mWindDirection = -1.;
		mWindSpeed = -1.;
		mSymbol = -1;
		mPercipitation = -1.;
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
