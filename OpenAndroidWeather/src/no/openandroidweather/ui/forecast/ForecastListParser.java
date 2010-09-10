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
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ForecastListParser {
	private final Context context;
	private Uri mUri;
	private final ContentResolver mContentResolver;
	private final LayoutInflater inflater;
	private IProgressItem progressItem;

	public ForecastListParser(final Context context,
			final IProgressItem progressItem) {
		super();
		this.context = context;
		this.progressItem = progressItem;
		mContentResolver = context.getContentResolver();
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private DateRow checkDate(final long from, final long to) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(from);
		final int oldDate = cal.get(Calendar.DATE);
		cal.setTimeInMillis(to);
		final int newDate = cal.get(Calendar.DATE);
		if (oldDate != newDate)
			return new DateRow(context, to);
		else
			return null;
	}

	public View getHeaderView(final Uri uri) {
		final Cursor c = mContentResolver.query(uri, null, null, null, null);
		c.moveToFirst();

		final View header = inflater.inflate(R.layout.forecast_view_header,
				null);
		final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG,
				DateFormat.SHORT);

		final String place = c.getString(c
				.getColumnIndex(WeatherContentProvider.Meta.PLACE_NAME));
		final TextView placeView = (TextView) header.findViewById(R.id.place);
		placeView.setText(place);

		final long downloaded = c.getLong(c
				.getColumnIndex(WeatherContentProvider.Meta.LOADED));
		((TextView) header.findViewById(R.id.downloaded)).setText(df
				.format(new Date(downloaded)));

		final long generated = c.getLong(c
				.getColumnIndexOrThrow(WeatherContentProvider.Meta.GENERATED));
		((TextView) header.findViewById(R.id.generated)).setText(df
				.format(new Date(generated)));

		final long nextForecast = c
				.getLong(c
						.getColumnIndexOrThrow(WeatherContentProvider.Meta.NEXT_FORECAST));
		((TextView) header.findViewById(R.id.next_update)).setText(df
				.format(new Date(nextForecast)));

		c.close();

		return header;
	}

	public List<IListRow> parseData(Uri uri) {
		mUri = uri;
		uri = Uri.withAppendedPath(mUri, ForecastListView.CONTENT_PATH);
		// Set start time to the beginning of this hour
		long lastHour = System.currentTimeMillis();
		lastHour -= 3600000;

		final String selection = ForecastListView.fromTime + ">" + lastHour;
		final Cursor c = mContentResolver.query(uri, null, selection, null,
				null);

		if (c == null || c.getCount() == 0) {
			c.close();
			throw new UnknownError("No data in table");
		}

		final List<IListRow> rows = new ArrayList<IListRow>();
		final int fromCol = c.getColumnIndexOrThrow(ForecastListView.fromTime);
		final int toCol = c.getColumnIndexOrThrow(ForecastListView.toTime);
		final int percipitationCol = c
				.getColumnIndexOrThrow(ForecastListView.percipitation);
		final int symbolCol = c.getColumnIndexOrThrow(ForecastListView.symbol);
		final int temperatureCol = c
				.getColumnIndexOrThrow(ForecastListView.temperature);
		final int windDirectionCol = c
				.getColumnIndexOrThrow(ForecastListView.windDirection);
		final int windSpeedCol = c
				.getColumnIndexOrThrow(ForecastListView.windSpeed);

		c.moveToFirst();
		long from = c.getLong(fromCol), to;
		int symbol;
		double percipitation, temperature, windDirection, windSpeed;
		rows.add(new DateRow(context, from));
		int length = c.getCount();
		for (int i = 0; i < length; i++) {
			from = c.getLong(fromCol);
			to = c.getLong(toCol);
			symbol = c.getInt(symbolCol);
			percipitation = c.getDouble(percipitationCol);
			temperature = c.getDouble(temperatureCol);
			windDirection = c.getDouble(windDirectionCol);
			windSpeed = c.getDouble(windSpeedCol);
			rows.add(new ForecastRow(symbol, temperature, percipitation,
					windSpeed, windDirection, inflater, from));
			final DateRow date = checkDate(from, to);
			if (date != null)
				rows.add(date);
			c.moveToNext();

		}
		c.close();

		return rows;
	}
}
