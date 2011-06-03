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

package no.openandroidweather.ui;

import java.util.Arrays;

import no.openandroidweather.R;
import no.openandroidweather.ui.stationpicker.StationPicker;
import no.openandroidweather.weathernotificatonservice.WeatherNotificationService;
import no.openandroidweather.wsklima.WsKlimaProxy;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class Settings extends Activity {
	private static final int ACTIVITY_CHOOSE_STATION = 1;
	public static final String LOG_ID = "no.firestorm.settings";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		setStationName();
		setChooseStationButtion();
		setGetWeatherButton();
		setUpdateRateSpinner();

	}

	private void setUpdateRateSpinner() {
		// Add spinner
		Spinner spinner = (Spinner) findViewById(R.id.updateRateSpinner);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.Update_rates, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setOnItemSelectedListener(new UpdateRateSelectedListener());
	    
	    // Find selected id
		SharedPreferences settings = getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		int updateRate = settings.getInt(
				WsKlimaProxy.PREFS_UPDATE_RATE_KEY,
				WsKlimaProxy.PREFS_UPDATE_RATE_DEFAULT);
		int[] updateRateArray = getResources().getIntArray(R.array.Update_rate_values);
		int id = Arrays.binarySearch(updateRateArray, updateRate);
		if (id>=0)
			spinner.setSelection(id);
		else
			spinner.setSelection(0);
	}

	private void setGetWeatherButton() {
		Button chooseStationButton = (Button) findViewById(R.id.get_weather);
		chooseStationButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				final Intent intent = new Intent(Settings.this,
						WeatherNotificationService.class);
				startService(intent);
			}
		});
	}

	private void setChooseStationButtion() {
		Button chooseStationButton = (Button) findViewById(R.id.choose_station);
		chooseStationButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				final Intent intent = new Intent(Settings.this,
						StationPicker.class);
				startActivityForResult(intent, ACTIVITY_CHOOSE_STATION);
			}
		});
	}

	private void setStationName() {
		TextView stationNameView = (TextView) findViewById(R.id.StationName);
		SharedPreferences settings = getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		String stationName = settings.getString(
				WsKlimaProxy.PREFS_STATION_NAME_KEY,
				WsKlimaProxy.PREFS_STATION_NAME_DEFAULT);
		stationNameView.setText(stationName);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// Handle chosen station
		switch (requestCode) {
		case ACTIVITY_CHOOSE_STATION:
			// Update station name if changed
			if (resultCode == RESULT_OK)
				setStationName();
			break;
		default:
			break;
		}
	}
	
	public class UpdateRateSelectedListener implements OnItemSelectedListener {
		@Override
	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {
			
			// Get update rate in minutes
			int updateRate = getResources().getIntArray(R.array.Update_rate_values)[(int) id];
			
			Editor settings = getSharedPreferences(
					WsKlimaProxy.PREFS_NAME, 0).edit();
			settings.putInt(WsKlimaProxy.PREFS_UPDATE_RATE_KEY, updateRate);
			settings.commit();
	    }

		@Override
	    public void onNothingSelected(AdapterView<?> parent) {}

	}


}
