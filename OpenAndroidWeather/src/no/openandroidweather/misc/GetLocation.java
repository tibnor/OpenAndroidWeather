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

import no.openandroidweather.weatherservice.WeatherService;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

public class GetLocation {
	public static Location getLocation(Context context) {
		Location loc = getLocation(context, Criteria.ACCURACY_FINE);

		if (loc != null)
			return loc;
		else
			return getLocation(context, Criteria.ACCURACY_COARSE);
	}

	private static Location getLocation(Context context, int accuracyCriteria) {
		final LocationManager locationManager = (LocationManager) context
				.getApplicationContext().getSystemService(
						Context.LOCATION_SERVICE);
		final Criteria criteria = new Criteria();
		criteria.setAccuracy(accuracyCriteria);
		String provider = locationManager.getBestProvider(criteria, true);

		if (provider == null) {
			return null;
		}

		return locationManager.getLastKnownLocation(provider);
	}
}
