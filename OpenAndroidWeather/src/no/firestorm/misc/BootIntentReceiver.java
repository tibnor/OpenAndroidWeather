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

import no.firestorm.weathernotificatonservice.WeatherNotificationService;
import no.firestorm.weathernotificatonservice.WeatherNotificationSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receive intent when phone has booted and start service.
 */
public class BootIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final int updateRate = WeatherNotificationSettings
				.getUpdateRate(context);
		if (updateRate > 0) {
			final Intent serviceIntent = new Intent(context,
					WeatherNotificationService.class);
			context.startService(serviceIntent);
		}
	}

}
