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

package no.firestorm.ui.stationpicker;

import java.util.HashMap;

import android.location.Location;

public class Station extends HashMap<String, String> implements
		Comparable<Station> {
	private static final long serialVersionUID = -1194829286083817084L;
	public static final String NAME = "name";
	public static final String DISTANCE = "distance";
	public static final String DIRECTION = "direction";

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public float getDistanceToCurrentPosition() {
		return distanceToCurrentPosition;
	}

	public float getBearingFromCurrentPotition() {
		return bearingToCurrentPotition;
	}

	private String name;
	private int id;
	private float distanceToCurrentPosition;
	private float bearingToCurrentPotition;

	public Station(String name, int id, final double latitude,
			final double longitude, Location currentPosition) {
		super();
		this.name = name;
		this.put(NAME, name);
		this.id = id;
		float[] result = new float[] { distanceToCurrentPosition,
				bearingToCurrentPotition };
		if (currentPosition != null) {
			Location.distanceBetween(currentPosition.getLatitude(),
					currentPosition.getLongitude(), latitude, longitude, result);
			this.distanceToCurrentPosition = result[0];
			this.bearingToCurrentPotition = result[1];
			this.put(DIRECTION, getDirection());
			this.put(DISTANCE, String.format("%.1f km",
					this.distanceToCurrentPosition / 1000));
		}
		else{
			this.distanceToCurrentPosition = 0;
			this.bearingToCurrentPotition = 0;
			this.put(DIRECTION, "");
			this.put(DISTANCE, "");
		}
	}

	public String getDirection() {
		float deg = bearingToCurrentPotition;
		if ((deg > 0 && deg <= 22.5))
			return "N";
		else if (deg <= 22.5 + 45)
			return "NE";
		else if (deg <= 22.5 + 90)
			return "E";
		else if (deg <= 22.5 + 135)
			return "SE";
		else if (deg <= 22.5 + 180)
			return "S";
		else if (deg <= 22.5 + 225)
			return "SW";
		else if (deg <= 22.5 + 270)
			return "W";
		else if (deg <= 22.5 + 315)
			return "NW";
		else
			return "N";

	}

	@Override
	public int compareTo(Station another) {
		return (int) (distanceToCurrentPosition - another.distanceToCurrentPosition);
	}

}
