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

import android.app.Service;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Listen for location updates, when accuracy is better than the distance
 * between the two closes stations, it will find the closest station and set
 * it in WsKlimaProxy. Remove itself as a location listener and set
 * stationReady=true, before it notify the service.
 * 
 */
public class UpdateLocation implements LocationListener {
	private Location mLocation;
	private UpdateLocationListener mListener;
	private Context mContext;

	public UpdateLocation(UpdateLocationListener listener, Context context) {
		mListener = listener;
		mContext = context;
	}

	public Location getLocation() {
		return mLocation;
	}

	@Override
	public void onLocationChanged(Location location) {
		mLocation = location;

		if (mListener.isAccurateEnough(location)) {
			stop();
			mListener.locationUpdated(location);
		}

	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void removeFromBroadcaster() {
		final LocationManager locManager = (LocationManager) mContext
				.getSystemService(Service.LOCATION_SERVICE);
		locManager.removeUpdates(this);
	}

	public void stop() {
		removeFromBroadcaster();
	}

}
