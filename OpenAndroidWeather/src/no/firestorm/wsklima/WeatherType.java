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

/**
 * Weather types that are measured
 */
public enum WeatherType {
	/** Temperature measured in Celsius */
	temperature,
	/** Max temperature measured in Celsius during day*/
	temperatureMax,
	/** Min temperature measured in Celsius during day*/
	temperatureMin,
	/** Wind speed in m/s */
	windSpeed,
	/** Wind direction in degrees */
	windDirection,
	/** Precipitation last hour in mm */
	precipitationLastHour,
	/** Precipitation last 12 hours in mm */
	precipitationLast12h
}
