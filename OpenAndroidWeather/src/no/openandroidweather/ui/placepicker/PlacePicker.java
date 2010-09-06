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

package no.openandroidweather.ui.placepicker;

import no.openandroidweather.R;
import no.openandroidweather.ui.addplace.AddPlaceActivity;
import no.openandroidweather.ui.forecast.ForecastListActivity;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.Place;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class PlacePicker extends ListActivity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.place_picker);

		TextView getNearestPlaceRow = (TextView) findViewById(R.id.place_name);
		getNearestPlaceRow.setText(R.string.current_location);
		getNearestPlaceRow.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(PlacePicker.this,
						ForecastListActivity.class));
			}
		});

		final Uri uri = Uri.withAppendedPath(
				WeatherContentProvider.CONTENT_URI, Place.CONTENT_PATH);
		final String[] projection = new String[] { Place._ID, Place.PLACE_NAME };
		final Cursor mCursor = managedQuery(uri, projection, null, null, null);

		final Button addPlaceButton = (Button) findViewById(R.id.add_place);
		addPlaceButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				startActivity(new Intent(PlacePicker.this,
						AddPlaceActivity.class));
			}
		});
		final String[] from = new String[] { Place.PLACE_NAME };
		final int[] to = new int[] { R.id.place_name };
		SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this,
				R.layout.place_picker_row, mCursor, from, to);
		setListAdapter(mAdapter);
		final ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(final AdapterView<?> parent,
					final View view, final int position, final long id) {
				final Intent intent = new Intent(PlacePicker.this,
						ForecastListActivity.class);
				intent.putExtra(ForecastListActivity._ROW_ID, id);
				startActivity(intent);
			}
		});

		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, final long id) {
				deletePlaceDialog(mCursor, id);
				return true;
			}
		});
	}

	/**
	 * Opens dialog for deleting place and delete it if requested
	 * 
	 * @param mCursor
	 * @param id
	 */
	private void deletePlaceDialog(final Cursor mCursor, final long id) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(
				PlacePicker.this);
		builder.setMessage(R.string.delete_place);
		builder.setCancelable(true);
		builder.setPositiveButton(getResources()
				.getString(R.string.yes),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						Uri url = Uri.withAppendedPath(
								WeatherContentProvider.CONTENT_URI,
								Place.CONTENT_PATH);
						url = Uri.withAppendedPath(url, id + "");
						getContentResolver().delete(url, null, null);
						mCursor.requery();
						dialog.cancel();
					}
				});
		builder.setNegativeButton(
				getResources().getString(R.string.no),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						dialog.cancel();
					}
				});
		builder.show();
	}
}
