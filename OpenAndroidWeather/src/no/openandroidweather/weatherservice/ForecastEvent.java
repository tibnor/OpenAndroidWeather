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

import java.util.EventObject;

public class ForecastEvent extends EventObject {
	private static final long serialVersionUID = 1L;
	// Status of the forecast. Between 0 and 1, 1 if completed
	private long status;
	// Id of the forecast in the database.
	private int forecastId;

	public ForecastEvent(Object source) {
		super(source);
		// TODO Auto-generated constructor stub
		throw new UnsupportedOperationException("Not implemented!");
	}

	public long getStatus() {
		return status;
	}

	public void setStatus(long status) {
		this.status = status;
	}

	public int getForecastId() {
		return forecastId;
	}

	public void setForecastId(int forecastId) {
		this.forecastId = forecastId;
	}

}
