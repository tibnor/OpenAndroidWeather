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

package no.openandroidweather.ui.preferences;

import no.openandroidweather.R;
import no.openandroidweather.misc.WeatherPreferences;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class WeatherPreferencesActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.weather_preferences);

		Preference numberOfDownloadedPreference = findPreference("number_of_downloaded_forecasts");
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		int downloads = preferences.getInt(
				WeatherPreferences.NUMBER_OF_DOWNLOADED_FORECASTS,
				WeatherPreferences.NUMBER_OF_DOWNLOADED_FORECASTS_DEFAULT);
		numberOfDownloadedPreference.setSummary(downloads+"");
	}
}
