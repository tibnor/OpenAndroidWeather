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

package no.firestorm.misc;

import no.firestorm.weathernotificatonservice.WeatherNotificationSettings;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class CheckInternetStatus {
	/**
	 * Check if either DownloadOnlyOnWifi is false or true and on wifi.
	 * 
	 * @param context
	 * @return true if it is okey to connect to internet
	 */
	public static boolean canConnectToInternet(Context context) {
		if (WeatherNotificationSettings.getDownloadOnlyOnWifi(context))
			return isWifiConnected(context);
		else
			return isConnected(context);
	}

	/**
	 * Return true is connected to internet
	 * 
	 * @param context
	 * @return true is connected to internet
	 */
	public static boolean isConnected(Context context) {
		final ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo info = manager.getActiveNetworkInfo();
		if (info != null)
			return info.isConnected();
		else
			return false;
	}

	/**
	 * Return true if wifi is connected
	 * 
	 * @param context
	 * @return true if wifi is connected
	 */
	public static boolean isWifiConnected(Context context) {
		final ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.isConnected();
	}
}
