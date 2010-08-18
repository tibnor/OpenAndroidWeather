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

interface IForecastEventListener{
	/**
	 * When a new forecast is ready.
	 * 
	 * @param event
	 */
	void newForecast(int rowId);

	/**
	 * When there is a new status for the event
	 * 
	 * @param event
	 */
	void newStatus(double status,int rowId);

	/**
	 * Check if the location is in target zone.
	 * 
	 * @param location
	 *            of the forecast
	 * @return true if in target zone
	 */
	boolean isInTargetZone(int rowId, double latitude, double longitude, double hightAboveSeaLevel);

	/**
	 * 
	 * @return Latitude of desired point of the forecast.
	 */
	double targetPointLatitude();
	
	/**
	 * 
	 * @return Longitude of desired point of the forecast.
	 */
	double targetPointLongitude();

	/**
	 * 
	 * @return The radius of the target zone in meter.
	 */
	int radiusOfTargetZone();

	/**
	 * @return Minimum time to next update of forecast in ms since 1970
	 *         (unixtime in milli seconds)
	 */
	int earliestNextUpdate();

	/**
	 * 
	 * @param time
	 *            to expected new forecast
	 */
	void setExpectedNextUpdate(int time);

	void exceptionOccured(int errorcode);
}