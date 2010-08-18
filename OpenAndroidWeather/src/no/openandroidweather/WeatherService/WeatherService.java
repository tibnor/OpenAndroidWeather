package no.openandroidweather.WeatherService;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

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

/**
 * This service should get the forecast from yr.no, via event listeners.
 */
public class WeatherService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented!");
	}

	// TODO Refactor the functions under so that they can be called to...

	private void removeNewForecastEventListener(ForecastEventListener listener) {
		throw new UnsupportedOperationException("Not implemented!");
	}

	/**
	 * Adds a forecast listener who is called when a new forecast is ready. If
	 * there is a forecast in the within the target zone, the listener will be
	 * called with a reference to the row in the database. If the forecast is
	 * old or there is no forecast in the database a new forecast is fetched
	 * from the provider.
	 * 
	 * @param listener
	 */
	private void addNewForecastEventListener(ForecastEventListener listener) {
		throw new UnsupportedOperationException("Not implemented!");
	}

	/**
	 * Force an update of forecasts or if id==null, all forecasts registered as ForecastEventListener. 
	 * @param id The row id's of the forecasts
	 */
	private void forceForcastUpdate(int[] id) {
		throw new UnsupportedOperationException("Not implemented!");
	}

}
