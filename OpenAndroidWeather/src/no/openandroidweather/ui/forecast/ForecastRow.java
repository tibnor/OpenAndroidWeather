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

import java.util.Calendar;
import java.util.Formatter;

import no.openandroidweather.R;
import no.openandroidweather.widget.Q;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ForecastRow implements IListRow {
	private static final String TAG = "ForecastRow";

	int symbol;

	String hour;
	double temperature;
	double percipitation;
	double windSpeed;
	double windDirection;
	LayoutInflater inflater;

	public ForecastRow(final int symbol, final double temperature,
			final double percipitation, final double windSpeed,
			final double windDirection, final LayoutInflater inflater,
			final long startTime) {
		super();
		this.symbol = symbol;
		this.temperature = temperature;
		this.percipitation = percipitation;
		this.windSpeed = windSpeed;
		this.windDirection = windDirection;
		this.inflater = inflater;
		Formatter formatter = new Formatter();
		hour = formatter.format("%tH", startTime).toString();
	}

	@Override
	public int getType() {
		return 1;
	}

	@Override
	public View getView(View convertView) {
		if (convertView == null)
			convertView = inflater.inflate(R.layout.forecast_view_item, null);
		
		

		// Set hour
		((TextView) convertView.findViewById(R.id.hour)).setText(hour);

		Log.i(TAG, "Symbol"+symbol);
		// Set symbol
		((ImageView) convertView.findViewById(R.id.symbol))
				.setImageResource(Q.symbol[symbol]);

		// Set temperature
		((TextView) convertView.findViewById(R.id.temperature))
				.setText(temperature + "°C");

		// Set precipitation
		((TextView) convertView.findViewById(R.id.percipitation))
				.setText(percipitation + " mm");

		// Set wind speed
		((TextView) convertView.findViewById(R.id.wind_speed))
				.setText(windSpeed + " m/s");

		// Set wind direction
		((TextView) convertView.findViewById(R.id.wind_direction))
				.setText(windDirection + "°");

		return convertView;
	}

}
