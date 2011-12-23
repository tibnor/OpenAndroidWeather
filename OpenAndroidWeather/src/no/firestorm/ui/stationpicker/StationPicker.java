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

package no.firestorm.ui.stationpicker;

import java.util.List;

import no.firestorm.R;
import no.firestorm.weathernotificatonservice.WeatherNotificationService;
import no.firestorm.weathernotificatonservice.WeatherNotificationSettings;
import no.firestorm.wsklima.WsKlimaProxy;
import no.firestorm.wsklima.database.WsKlimaDataBaseHelper;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * Activity for selecting and saving station
 */
public class StationPicker extends ListActivity {
	List<Station> mStations;
	List<Station> mStationsSortedAlphabetical = null;
	List<Station> mStationsSortedByDistance = null;
	boolean mSortByDistance = true;

	private final TextWatcher filterTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			((SimpleAdapter) getListAdapter()).getFilter().filter(s);
		}

	};

	private List<Station> addUseNearestStation(List<Station> stations) {
		stations.add(0,
				new Station(this.getString(R.string.use_nearest_station),
						WsKlimaProxy.FIND_NEAREST_STATION, 0, 0, null, true));
		return stations;
	}

	private Location getCurrentLocation() {
		final LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final List<String> locProviderNames = locMan.getProviders(true);
		Location location = null;
		for (final String locProvider : locProviderNames) {
			location = locMan.getLastKnownLocation(locProvider);
			if (location != null)
				break;
		}
		return location;
	}

	private void getWeather() {
		final Intent intent = new Intent(StationPicker.this,
				WeatherNotificationService.class);
		intent.putExtra(WeatherNotificationService.INTENT_EXTRA_ACTION,
				WeatherNotificationService.INTENT_EXTRA_ACTION_GET_TEMP);
		startService(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stationslist);
		setResult(RESULT_CANCELED);

		// Insert station
		updateAdapter();

		setGetWeatherButton();

		final ListView lw = getListView();
		lw.setTextFilterEnabled(true);

		final EditText filterText = (EditText) findViewById(R.id.search_box);
		filterText.addTextChangedListener(filterTextWatcher);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.stationslist, menu);
		return true;
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.sort:
			mSortByDistance = !mSortByDistance;
			updateAdapter();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		final MenuItem item = menu.getItem(0);
		if (!mSortByDistance) {
			item.setTitle(R.string.sort_by_distance);
			item.setIcon(android.R.drawable.ic_menu_sort_by_size);
		} else {
			item.setTitle(R.string.sort_by_name);
			item.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	private void saveStation(long id) {
		final SimpleAdapter adapter = (SimpleAdapter) getListView()
				.getAdapter();
		final Station station = (Station) adapter.getItem((int) id);
		id = station.getId();
		if (id != WsKlimaProxy.FIND_NEAREST_STATION) {
			// Get name
			final String name = station.getName();

			// save
			WeatherNotificationSettings.setStation(this, name, (int) id);
			WeatherNotificationSettings.setUseNearestStation(this, false);
		} else
			WeatherNotificationSettings.setUseNearestStation(this, true);

		final int updateRate = WeatherNotificationSettings.getUpdateRate(this);

		if (updateRate > 0) {
			final Intent intent = new Intent(this,
					WeatherNotificationService.class);
			intent.putExtra(WeatherNotificationService.INTENT_EXTRA_ACTION,
					WeatherNotificationService.INTENT_EXTRA_ACTION_GET_TEMP);
			startService(intent);
		}

	}

	private void setGetWeatherButton() {
		final ImageButton chooseStationButton = (ImageButton) findViewById(R.id.get_weather);
		chooseStationButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getWeather();
			}
		});
	}

	private void updateAdapter() {

		// Get stations
		final WsKlimaDataBaseHelper dbhelper = new WsKlimaDataBaseHelper(this);
		final Location loc = getCurrentLocation();
		if (loc == null) {
			final Toast toast = Toast.makeText(this,
					R.string.error_could_not_find_your_position_,
					Toast.LENGTH_SHORT);
			toast.show();
			mSortByDistance = false;
		}

		if (mSortByDistance)
			if (mStationsSortedByDistance == null) {
				mStations = addUseNearestStation(dbhelper
						.getStationsSortedByLocation(loc, true));
				mStationsSortedByDistance = mStations;
			} else
				mStations = mStationsSortedByDistance;
		else if (mStationsSortedAlphabetical == null) {
			mStations = addUseNearestStation(dbhelper
					.getStationsSortedAlphabetic(loc, true));
			mStationsSortedAlphabetical = mStations;
		} else
			mStations = mStationsSortedAlphabetical;

		// set adapter
		final String[] from = { Station.NAME, Station.DISTANCE,
				Station.DIRECTION };
		final int[] to = { R.id.StationName, R.id.Distance, R.id.Direction };
		final SimpleAdapter adapter = new SimpleAdapter(this, mStations,
				R.layout.stationslist_row, from, to);

		// Add to list view
		setListAdapter(adapter);

	}
}
