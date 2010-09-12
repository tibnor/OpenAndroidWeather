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

package no.openandroidweather.weatherservice;

import no.openandroidweather.weatherservice.IForecastEventListener;


/* See http://github.com/commonsguy/cw-andtutorials/blob/master/19-RemoteService/Patchy/src/apt/tutorial/ 
 * for how to handle this.
 */
interface IWeatherService{
	/**
	 * Adds a forecast listener who is called when a new forecast is ready. If
	 * there is a forecast in the within the target zone, the listener will be
	 * called with a reference to the row in the database. If the forecast is
	 * old or there is no forecast in the database a new forecast is fetched
	 * from the provider.
	 * 
	 * @param listener for callbacks
	 * @param latitude of the forecast
	 * @param longitude of the forecast
	 * @param altitude of the forecast
	 * @param tolleranceRadius A horizontal tolerance radius, if there are already forecasts who are downloaded within this radius,
	 *			 this forecast will be returned
	 * @param tolleranceVerticalDistance A vertical tolerance distance.
	 */
	void getForecast(IForecastEventListener listener, double latitude, double longitude, 
			double altitude, double toleranceRadius, double toleranceVerticalDistance);
			
	/**
	 * Downloads the forecast who is closest to to the current position of the device.
	 *  
	 *
	 * @param listener for callbacks
	 * @param tolleranceRadius A horizontal tolerance radius, if there are already forecasts who are downloaded within this radius,
	 *			 this forecast will be returned
	 * @param tolleranceVerticalDistance A vertical tolerance distance.
	 */
	void getNearestForecast(IForecastEventListener listener, double toleranceRadius, double toleranceVerticalDistance);
}