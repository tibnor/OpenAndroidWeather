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

package no.openandroidweather.ui.addplace;

import no.openandroidweather.R;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.Place;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class PlaceAdapter extends CursorAdapter {
	private int mPlaceNameCol;

	public PlaceAdapter(Context context, Cursor c) {
		super(context, c);
		mPlaceNameCol = c.getColumnIndexOrThrow(Place.PLACE_NAME);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView placeNameView = (TextView) view.findViewById(R.id.place_name);
		placeNameView.setText(cursor.getString(mPlaceNameCol));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.place_picker_row, null);
		return view;
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}
}
