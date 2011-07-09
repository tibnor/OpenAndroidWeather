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

package no.firestorm.ui;

import java.util.Arrays;

import no.firestorm.R;
import no.firestorm.ui.stationpicker.StationPicker;
import no.firestorm.weathernotificatonservice.WeatherNotificationService;
import no.firestorm.wsklima.WsKlimaProxy;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Startup activity
 * 
 */
public class Settings extends Activity {
	/**
	 * Callback class for update rate scroller
	 */
	public class UpdateRateSelectedListener implements OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {

			// Get update rate in minutes
			final int updateRate = getResources().getIntArray(
					R.array.Update_rate_values)[(int) id];

			WsKlimaProxy.setUpdateRate(Settings.this, updateRate);

		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}

	}

	private static final int ACTIVITY_CHOOSE_STATION = 1;
	@SuppressWarnings("unused")
	private static final String LOG_ID = "no.firestorm.settings";
	private static final String PREF_NAME = "first app run";

	private static final String PREF_FIRST_RUN = "first app run";

	private void checkIfFirstRun() {
		final SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
		final boolean firstRun = settings.getBoolean(PREF_FIRST_RUN, true);
		if (firstRun) {
			getWeather();
			final Editor edit = settings.edit();
			edit.putBoolean(PREF_FIRST_RUN, false);
			edit.commit();
		}
	}

	private void getWeather() {
		final Intent intent = new Intent(Settings.this,
				WeatherNotificationService.class);
		intent.putExtra(WeatherNotificationService.INTENT_EXTRA_ACTION,
				WeatherNotificationService.INTENT_EXTRA_ACTION_GET_TEMP);
		startService(intent);
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
			// getWeather();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		checkIfFirstRun();
		setStationName();
		setChooseStationButtion();
		setGetWeatherButton();
		setUpdateRateSpinner();
		setRateButton();

	}

	private void setRateButton() {
		final Button rateButton = (Button) findViewById(R.id.rate);
		rateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				openRateWindow();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about:
			openAboutBox();
			break;
		case R.id.rate:
			openRateWindow();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void openRateWindow() {
		Intent goToMarket = null;
		goToMarket = new Intent(Intent.ACTION_VIEW,
				Uri.parse("market://details?id=no.firestorm"));
		startActivity(goToMarket);
	}

	private void openAboutBox() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.about_text)
				.setCancelable(true)
				.setNeutralButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				})
				.setPositiveButton(R.string.donate,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								final String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=W66JKFZDLHFF4&lc=NO&item_name=firestorm&item_number=inapp&currency_code=NOK&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted";
								final Intent i = new Intent(Intent.ACTION_VIEW);
								i.setData(Uri.parse(url));
								startActivity(i);
							}
						}).setTitle(R.string.about);

		final AlertDialog alert = builder.create();
		alert.show();
	}

	private void setChooseStationButtion() {
		final Button chooseStationButton = (Button) findViewById(R.id.choose_station);
		chooseStationButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final Intent intent = new Intent(Settings.this,
						StationPicker.class);
				startActivityForResult(intent, ACTIVITY_CHOOSE_STATION);
			}
		});
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

	private void setStationName() {
		final TextView stationNameView = (TextView) findViewById(R.id.StationName);
		String stationName;
		if (WsKlimaProxy.isUsingNearestStation(this)) {
			stationName = getString(R.string.use_nearest_station);
		} else {
			stationName = WsKlimaProxy.getStationName(this);
		}

		stationNameView.setText(stationName);
	}

	private void setUpdateRateSpinner() {
		// Add spinner
		final Spinner spinner = (Spinner) findViewById(R.id.updateRateSpinner);
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter
				.createFromResource(this, R.array.Update_rates,
						R.layout.updaterate_spinner_view);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new UpdateRateSelectedListener());

		// Find selected update rate
		final int updateRate = WsKlimaProxy.getUpdateRate(this);
		final int[] updateRateArray = getResources().getIntArray(
				R.array.Update_rate_values);
		final int id = Arrays.binarySearch(updateRateArray, updateRate);
		if (id >= 0)
			spinner.setSelection(id);
		else
			spinner.setSelection(0);
	}

}
