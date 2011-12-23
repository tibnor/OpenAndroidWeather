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

package no.firestorm.weathernotificatonservice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

/**
 * Wait for internet to connect, when it connect it update the weather
 * notification and disables the receiver
 * 
 */
public class InternetEnabledIntentReceiver extends BroadcastReceiver {

	/**
	 * Enable or disable the receiver
	 * 
	 * @param context
	 * @param enable
	 */
	public static void setEnableReciver(Context context, boolean enable) {
		final ComponentName componentName = new ComponentName(context,
				InternetEnabledIntentReceiver.class);
		int flag;
		if (enable)
			flag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
		else
			flag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

		context.getPackageManager().setComponentEnabledSetting(componentName,
				flag, PackageManager.DONT_KILL_APP);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			final NetworkInfo info = (NetworkInfo) intent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
				final Intent serviceIntent = new Intent(context,
						WeatherNotificationService.class);
				intent.putExtra(WeatherNotificationService.INTENT_EXTRA_ACTION,
						WeatherNotificationService.INTENT_EXTRA_ACTION_GET_TEMP);
				context.startService(serviceIntent);
				setEnableReciver(context, false);
			}
		}
	}

}
