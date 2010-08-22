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
	 * @param uri to the new forecast
	 */
	void newForecast(String uri,long forecastGenerated);

	/**
	 * When there is a new progress in getting the forecast
	 * 
	 * @param progress new progress, between 0 and 1, where 1 is completed.
	 */
	void progress(double progress);

	/**
	 * New expected time for the next forecast, check database.
	 */
	void newExpectedTime();
	
	/**
	 * An exception occurred
	 */
	void exceptionOccurred(int errorcode);
}