/*
	Copyright 2011 Torstein Ingebrigtsen Bø

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

package no.openandroidweather.ui.stationpicker;

import no.openandroidweather.R;
import no.openandroidweather.wsklima.WsKlimaProxy;
import no.openandroidweather.wsklima.database.WsKlimaDataBaseHelper;
import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class StationPicker extends ListActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stationslist);
		setResult(RESULT_CANCELED);

		// Get cursor
		StationQuery stationQuery = new StationQuery(this);
		Cursor c = stationQuery.runQuery("");
		startManagingCursor(c);

		// Set cursorAdapter
		String[] from = { WsKlimaDataBaseHelper.STATIONS_NAME };
		int[] to = { R.id.StationName };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.stationslist_row, c, from, to);

		// Set filter adater
		adapter.setFilterQueryProvider(stationQuery);

		// Add to list view
		setListAdapter(adapter);
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		// Save station
		saveStation(id);

		// Return
		setResult(RESULT_OK);
		finish();
	}

	private void saveStation(long id) {
		// find name
		SQLiteDatabase db = new WsKlimaDataBaseHelper(this)
				.getReadableDatabase();
		String[] select = { WsKlimaDataBaseHelper.STATIONS_NAME };
		String selection = WsKlimaDataBaseHelper.STATIONS_ID + " = " + id;
		Cursor c = db.query(WsKlimaDataBaseHelper.STATIONS_TABLE_NAME, select,
				selection, null, null, null, null);

		// Check that station exists
		if (c.getCount() != 1)
			throw new UnknownError("Could not find station");

		// Get name
		c.moveToFirst();
		String name = c.getString(c
				.getColumnIndexOrThrow(WsKlimaDataBaseHelper.STATIONS_NAME));
		c.close();

		// save
		Editor settings = getSharedPreferences(WsKlimaProxy.PREFS_NAME, 0)
				.edit();
		settings.putInt(WsKlimaProxy.PREFS_STATION_ID_KEY, (int) id);
		settings.putString(WsKlimaProxy.PREFS_STATION_NAME_KEY, name);
		settings.commit();
	}

	class StationQuery implements FilterQueryProvider {
		SQLiteDatabase mDb;

		public StationQuery(Context context) {
			WsKlimaDataBaseHelper dbHelper = new WsKlimaDataBaseHelper(context);
			mDb = dbHelper.getReadableDatabase();
		}

		@Override
		public Cursor runQuery(CharSequence constraint) {
			String[] select = { WsKlimaDataBaseHelper.STATIONS_ID,
					WsKlimaDataBaseHelper.STATIONS_NAME };
			String orderBy = WsKlimaDataBaseHelper.STATIONS_NAME;
			String selection = WsKlimaDataBaseHelper.STATIONS_NAME + " LIKE '%"
					+ constraint.toString().toUpperCase() + "%'";
			return mDb.query(WsKlimaDataBaseHelper.STATIONS_TABLE_NAME, select,
					selection, null, null, null, orderBy);
		}

	}

}
