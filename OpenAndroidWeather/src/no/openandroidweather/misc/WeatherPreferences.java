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

package no.openandroidweather.misc;

/**
 * Constants for shared preferences
 * 
 */
public class WeatherPreferences {
	public static final String DOWNLOAD_ONLY_ON_WIFI = "download_only_on_wifi";
	public static final boolean DOWNLOAD_ONLY_ON_WIFI_DEFAULT = true;

	public static final String NUMBER_OF_DOWNLOADED_FORECASTS = "number_of_downloaded_forecasts";
	public static final int NUMBER_OF_DOWNLOADED_FORECASTS_DEFAULT = 0;
}
