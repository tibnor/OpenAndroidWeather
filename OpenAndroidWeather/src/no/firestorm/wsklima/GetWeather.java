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

package no.firestorm.wsklima;

import java.util.List;

import no.firestorm.ui.stationpicker.Station;
import no.firestorm.weathernotificatonservice.UpdateLocation;
import no.firestorm.weathernotificatonservice.UpdateLocationListener;
import no.firestorm.wsklima.database.WsKlimaDataBaseHelper;
import no.firestorm.wsklima.exception.NoLocationException;

import org.apache.http.HttpException;

import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;

public class GetWeather implements UpdateLocationListener {
	private Float accuracyDemand = Float.MAX_VALUE;
	private Context mContext = null;
	private final static int LOCATION_MAX_AGE = 5 * 60 * 1000;
	private Location mLocation = null;
	private WsKlimaProxy mProxy;
	private WsKlimaDataBaseHelper mDb;
	private Station mStation = null;

	public GetWeather(Context context) {
		mContext = context;
		mProxy = new WsKlimaProxy();
		mDb = new WsKlimaDataBaseHelper(context);
	}

	/**
	 * Gets the temperature from the closest weather station that has a new
	 * weather report. If the closest station does not deliver a result, the
	 * second closest station is then asked and so on. The station is saved in
	 * the object and can be found by calling getStation()
	 * 
	 * @return Weather temperature
	 * @throws NetworkErrorException if it can't connect to the web service
	 * @throws NoLocationException if it can't get a location
	 * @throws HttpException if it get response from a webserver but it is corrupted (e.g., login page of public wlan)
	 */
	public WeatherElement getWeatherElement() throws NetworkErrorException,
			NoLocationException, HttpException {
		updateLocation();
		List<Station> stations = getNearestStations();
		for (Station station : stations) {
			WeatherElement weather = getWeather(station);
			if (weather != null) {
				mStation = station;
				return weather;
			}
		}
		return null;

	}

	private List<Station> getNearestStations() {
		return mDb.getStationsSortedByLocation(mLocation, true);
	}

	// Get weather from wsklimaproxy
	private WeatherElement getWeather(Station station)
			throws NetworkErrorException, HttpException {
		return mProxy.getTemperatureNow(station.getId(), mContext);

	}

	// Use location from mLocation and find the nearest station that is not
	// ignored in ignoredStationNumbers

	// Get the current location
	private void updateLocation() throws NoLocationException {
		// Check old results
		Location loc = getLastLocation();
		if (loc != null && isAccurateEnough(loc)) {
			mLocation = loc;
			return;
		}
		

		// Find location provider
		final LocationManager locMan = (LocationManager) mContext
				.getSystemService(Service.LOCATION_SERVICE);
		final Criteria criteria = new Criteria();
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		final String provider = locMan.getBestProvider(criteria, true);

		if (provider != null) {
			// Register provider
			UpdateLocation getLocation = new UpdateLocation(this, mContext);
			locMan.requestLocationUpdates(provider, 0, 0, getLocation,
					Looper.getMainLooper());

			// Wait for the updating of station to complete
			synchronized (this) {
				try {
					this.wait(2 * 60 * 1000);
				} catch (final InterruptedException e) {
					// If it did not get a good enough accuracy within 2
					// minutes, use the latest one
					getLocation.stop();
					loc = getLocation.getLocation();
					if (loc == null) {
						throw new NoLocationException(null);
					} else {
						mLocation = loc;
					}
				}
			}

		} else
			throw new NoLocationException(null);
	}

	/**
	 * Return station for weather, must be called after getWeatherElement
	 * 
	 * @return station
	 */
	public Station getStation() {
		return mStation;
	}

	@Override
	public void locationUpdated(Location loc) {
		mLocation = loc;
		synchronized (this) {
			this.notifyAll();
		}
	}

	private Location getLastLocation() {
		final LocationManager locman = (LocationManager) mContext
				.getSystemService(Context.LOCATION_SERVICE);
		final List<String> providers = locman.getAllProviders();

		for (final String p : providers) {
			final Location loc = locman.getLastKnownLocation(p);
			if (loc != null) {
				final long age = System.currentTimeMillis() - loc.getTime();
				if (age < LOCATION_MAX_AGE && isAccurateEnough(loc))
					return loc;
			}
		}
		return null;

	}

	public boolean isAccurateEnough(Location location) {

		// Check if location is within accuracy demand
		if (!location.hasAccuracy()){
			return true;
		} else if (location.getAccuracy() < accuracyDemand) {
			// update accuracy demand
			final WsKlimaDataBaseHelper db = new WsKlimaDataBaseHelper(mContext);
			final List<Station> stations = db.getStationsSortedByLocation(
					location, true);
			accuracyDemand = stations.get(1).getDistanceToCurrentPosition()
					- stations.get(0).getDistanceToCurrentPosition();
			db.close();

			if (location.getAccuracy() < accuracyDemand)
				return true;
		}
		return false;
	}

}
