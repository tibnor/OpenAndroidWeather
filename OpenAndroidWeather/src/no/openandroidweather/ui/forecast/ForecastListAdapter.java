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

import java.util.List;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

public class ForecastListAdapter implements ListAdapter {

	private final List<? extends IListRow> mRows;

	public ForecastListAdapter(final List<? extends IListRow> rows) {
		mRows = rows;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public int getCount() {
		if (mRows == null)
			return 0;
		else
			return mRows.size();
	}

	@Override
	public Object getItem(final int position) {
		return mRows.get(position);
	}

	@Override
	public long getItemId(final int position) {
		return position;
	}

	@Override
	public int getItemViewType(final int position) {
		return mRows.get(position).getType();
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		return mRows.get(position).getView(convertView);
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return mRows == null || mRows.isEmpty();
	}

	@Override
	public boolean isEnabled(final int position) {
		return false;
	}

	@Override
	public void registerDataSetObserver(final DataSetObserver observer) {
		// TODO Auto-generated method stub
	}

	@Override
	public void unregisterDataSetObserver(final DataSetObserver observer) {
		// TODO Auto-generated method stub
	}

}
