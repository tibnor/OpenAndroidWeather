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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import no.openandroidweather.R;
import no.openandroidweather.misc.IProgressItem;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.ForecastListView;
import no.openandroidweather.weathercontentprovider.WeatherType;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ForecastListParser {
	private final Context context;
	private Uri mUri;
	private long fromTime = Long.MIN_VALUE;
	private long toTime = Long.MIN_VALUE;
	private boolean hasAll = false;
	private int symbol = -1;
	private double percipitation = -1;
	private final double uknownTemperature = Double.MIN_VALUE;
	private double temperature = uknownTemperature;
	private double windSpeed = -1.;
	private double windDirection = -1;
	private IProgressItem progressItem;
	private final ContentResolver mContentResolver;
	private LayoutInflater inflater;

	public ForecastListParser(final Context context, IProgressItem progressItem) {
		super();
		this.context = context;
		this.progressItem = progressItem;
		mContentResolver = context.getContentResolver();
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	/**
	 * Insert a new row to rows
	 */
	private void addRow() {
		ContentValues values = new ContentValues();
		values.put(ForecastListView.fromTime, fromTime);
		values.put(ForecastListView.toTime, toTime);
		values.put(ForecastListView.percipitation, percipitation);
		values.put(ForecastListView.symbol, symbol);
		values.put(ForecastListView.temperature, temperature);
		values.put(ForecastListView.windDirection, windDirection);
		values.put(ForecastListView.windSpeed, windSpeed);
		Uri uri = Uri.withAppendedPath(mUri,
				WeatherContentProvider.ForecastListView.CONTENT_PATH);
		mContentResolver.insert(uri, values);
	}

	private DateRow checkDate(long from, long to) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(from);
		final int oldDate = cal.get(Calendar.DATE);
		cal.setTimeInMillis(to);
		final int newDate = cal.get(Calendar.DATE);
		if (oldDate != newDate) {
			return new DateRow(context, to);
		} else
			return null;
	}

	private boolean checkHasAll() {
		return !(symbol < 0 || percipitation < 0
				|| temperature == uknownTemperature || windSpeed < 0 || windDirection < 0);
	}

	/**
	 * 
	 */
	private void convertForecastToForecastListView() {
		Uri uri;
		String selection;
		Cursor c;
		uri = Uri.withAppendedPath(mUri,
				WeatherContentProvider.Forecast.CONTENT_DIRECTORY);
		final String[] projection = { WeatherContentProvider.Forecast.FROM,
				WeatherContentProvider.Forecast.TO,
				WeatherContentProvider.Forecast.VALUE,
				WeatherContentProvider.Forecast.TYPE };
		final String sortOrder = WeatherContentProvider.Forecast.FROM + ", "
				+ WeatherContentProvider.Forecast.TO + " ASC";

		// Set start time to the beginning of this hour
		fromTime = System.currentTimeMillis();
		fromTime -= fromTime % 3600000;

		// Include only forecast from the beginning of this hour
		selection = WeatherContentProvider.Forecast.FROM + ">=" + fromTime;
		// Gets data
		c = context.getContentResolver().query(uri, projection, selection,
				null, sortOrder);

		final int fromTimeCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.Forecast.FROM);
		final int toTimeCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.Forecast.TO);
		final int valueCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.Forecast.VALUE);
		final int typeCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.Forecast.TYPE);
		c.moveToFirst();

		reset();
		final int length = c.getCount();
		for (int i = 0; i < length; i++) {
			if (progressItem != null) {
				progressItem.progress(500 + (500 * i) / length);
			}

			// Gets data
			final long from = c.getLong(fromTimeCol);
			// Not needed?
			toTime = c.getLong(toTimeCol);

			if (fromTime < from) {
				// for one point there is no forecast for all values it is
				// ignored.
				if (hasAll) {
					addRow();
				}
				reset();
				fromTime = from;
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
	}

	public View getHeaderView(Uri uri) {
		Cursor c = mContentResolver.query(uri, null, null, null, null);
		c.moveToFirst();
		
		View header = inflater.inflate(R.layout.forecast_view_header, null);
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
		
		String place = c.getString(c.getColumnIndex(WeatherContentProvider.Meta.PLACE_NAME));
		TextView placeView = (TextView) header.findViewById(R.id.place);
		placeView.setText(place);
		
		long downloaded = c.getLong(c.getColumnIndex(WeatherContentProvider.Meta.LOADED));
		((TextView) header.findViewById(R.id.downloaded)).setText(df.format(new Date(downloaded)));
		
		long generated = c.getLong(c.getColumnIndexOrThrow(WeatherContentProvider.Meta.GENERATED));
		((TextView) header.findViewById(R.id.generated)).setText(df.format(new Date(generated)));
		
		long nextForecast = c.getLong(c.getColumnIndexOrThrow(WeatherContentProvider.Meta.NEXT_FORECAST));
		((TextView) header.findViewById(R.id.next_update)).setText(df.format(new Date(nextForecast)));
		
		c.close();
		
		return header;
	}

	public List<IListRow> parseData(Uri uri) {
		mUri = uri;
		uri = Uri.withAppendedPath(mUri, ForecastListView.CONTENT_PATH);
		// Set start time to the beginning of this hour
		long lastHour = System.currentTimeMillis();
		lastHour -= 3600000;
		
		String selection = ForecastListView.fromTime + ">"
				+ lastHour;
		Cursor c = mContentResolver.query(uri, null, selection, null, null);
		
		if (c == null || c.getCount() == 0) {
			c.close();
			convertForecastToForecastListView();
			return parseData(mUri);
		}
		
		List<IListRow> rows = new ArrayList<IListRow>();
		int fromCol = c.getColumnIndexOrThrow(ForecastListView.fromTime);
		int toCol = c.getColumnIndexOrThrow(ForecastListView.toTime);
		int percipitationCol = c
				.getColumnIndexOrThrow(ForecastListView.percipitation);
		int symbolCol = c.getColumnIndexOrThrow(ForecastListView.symbol);
		int temperatureCol = c
				.getColumnIndexOrThrow(ForecastListView.temperature);
		int windDirectionCol = c
				.getColumnIndexOrThrow(ForecastListView.windDirection);
		int windSpeedCol = c.getColumnIndexOrThrow(ForecastListView.windSpeed);

		c.moveToFirst();
		long from = c.getLong(fromCol), to;
		int symbol;
		double percipitation, temperature, windDirection, windSpeed;
		rows.add(new DateRow(context, from));
		for (int i = 0; i < c.getCount(); i++) {
			from = c.getLong(fromCol);
			to = c.getLong(toCol);
			symbol = c.getInt(symbolCol);
			percipitation = c.getDouble(percipitationCol);
			temperature = c.getDouble(temperatureCol);
			windDirection = c.getDouble(windDirectionCol);
			windSpeed = c.getDouble(windSpeedCol);
			rows.add(new ForecastRow(symbol, temperature, percipitation,
					windSpeed, windDirection, inflater, from));
			DateRow date = checkDate(from, to);
			if (date != null)
				rows.add(date);
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
