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
import java.util.Date;

import no.openandroidweather.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class DateRow implements IListRow {
	LayoutInflater mInflater;
	String mDate;

	public DateRow(final Context context, final long date) {
		super();
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
		mDate = df.format(new Date(date));
	}

	@Override
	public int getType() {
		return 0;
	}

	@Override
	public View getView(View convertView) {
		if (convertView == null)
			convertView = mInflater.inflate(R.layout.forecast_view_date_item,
					null);
		final TextView text = (TextView) convertView;
		text.setText(mDate);
		return convertView;
	}

}
