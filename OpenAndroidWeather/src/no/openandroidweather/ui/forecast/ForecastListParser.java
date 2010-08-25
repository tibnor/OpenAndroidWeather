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

package no.openandroidweather.ui.forecast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import no.openandroidweather.misc.IProgressItem;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weathercontentprovider.WeatherType;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;

public class ForecastListParser {
	private final Context context;

	private long startTime = Long.MIN_VALUE;
	private boolean hasAll = false;
	private int symbol = -1;
	private double percipitation = -1;
	private final double uknownTemperature = Double.MIN_VALUE;
	private double temperature = uknownTemperature;
	private double windSpeed = -1.;
	private double windDirection = -1;
	private ArrayList<IListRow> rows;
	private IProgressItem progressItem;

	public ForecastListParser(final Context context, IProgressItem progressItem) {
		super();
		this.context = context;
		this.progressItem = progressItem;
	}

	/**
	 * Insert a new row to rows
	 */
	private void addRow() {
		final LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final ForecastRow row = new ForecastRow(symbol, temperature,
				percipitation, windSpeed, windDirection, inflater, startTime);
		rows.add(row);
	}

	private boolean checkHasAll() {
		return !(symbol < 0 || percipitation < 0
				|| temperature == uknownTemperature || windSpeed < 0 || windDirection < 0);
	}

	/**
	 * Check if it is a new day, if so it insert a DateRow
	 * 
	 * @param oldFrom
	 * @param newFrom
	 */
	private void checkNewDate(final long oldFrom, final long newFrom) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(oldFrom);
		final int oldDate = cal.get(Calendar.DATE);
		cal.setTimeInMillis(newFrom);
		final int newDate = cal.get(Calendar.DATE);
		if (oldDate != newDate) {
			final DateRow row = new DateRow(context, newFrom);
			rows.add(row);
		}
	}

	public List<IListRow> parseData(Uri uri) {
		uri = Uri.withAppendedPath(uri,
				WeatherContentProvider.FORECAST_CONTENT_DIRECTORY);
		final String[] projection = { WeatherContentProvider.FORECAST_FROM,
				WeatherContentProvider.FORECAST_TO,
				WeatherContentProvider.FORECAST_VALUE,
				WeatherContentProvider.FORECAST_TYPE };
		final String sortOrder = WeatherContentProvider.FORECAST_FROM + ", "
				+ WeatherContentProvider.FORECAST_TO + " ASC";

		// Set start time to the beginning of this hour
		startTime = System.currentTimeMillis();
		startTime -= startTime % 3600000;

		// Include only forecast from the beginning of this hour
		final String selection = WeatherContentProvider.FORECAST_FROM + ">="
				+ startTime;
		// Gets data
		final Cursor c = context.getContentResolver().query(uri, projection,
				selection, null, sortOrder);
		rows = new ArrayList<IListRow>();

		final int startTimeCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.FORECAST_FROM);
		final int stopTimeCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.FORECAST_TO);
		final int valueCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.FORECAST_VALUE);
		final int typeCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.FORECAST_TYPE);
		c.moveToFirst();

		reset();
		final int length = c.getCount();
		for (int i = 0; i < length; i++) {
			if(progressItem != null){
				progressItem.progress(500+(500*i)/length);
			}
			
			// Gets data
			final long from = c.getLong(startTimeCol);
			// Not needed?
			// long to = c.getLong(stopTimeCol);

			if (startTime < from) {
				// for one point there is no forecast for all values it is ignored.
				if (hasAll) {
					addRow();
				}
				reset();
				checkNewDate(startTime, from);
				startTime = from;
			}

			if (!hasAll) {
				// Since cursor is ordered by to time the latest stop time
				final String value = c.getString(valueCol);
				final int type = c.getInt(typeCol);

				switch (type) {
				case WeatherType.symbol:
					symbol = new Integer(value);
					break;
				case WeatherType.precipitation:
					percipitation = new Double(value);
					break;
				case WeatherType.temperature:
					temperature = new Double(value);
					break;
				case WeatherType.windSpeed:
					windSpeed = new Double(value);
					break;
				case WeatherType.windDirection:
					windDirection = new Double(value);
				}

				if (checkHasAll())
					hasAll = true;
			}
			c.moveToNext();
		}
		c.close();

		return rows;
	}

	private void reset() {
		hasAll = false;
		symbol = -1;
		temperature = uknownTemperature;
		windDirection = -1.;
		windSpeed = -1.;
		percipitation = -1.;
	}
}
