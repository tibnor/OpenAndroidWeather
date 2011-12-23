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

import no.firestorm.wsklima.database.WsKlimaDataBaseHelper;
import android.content.Context;
import android.location.Location;

/**
 * Weather station, with name, id and position
 */
public class Station extends HashMap<String, String> implements
		Comparable<Station> {
	private static final long serialVersionUID = -1194829286083817084L;
	/** Key in map for station name */
	public static final String NAME = "name";
	/** Key in map for distance between current position and station */
	public static final String DISTANCE = "distance";
	/** Key in map for direction from current position to station */
	public static final String DIRECTION = "direction";

	private String name;
	private int id;
	private float distanceToCurrentPosition;
	private float bearingToCurrentPotition;
	private boolean isReliable = true;

	/**
	 * @param name
	 *            of station
	 * @param id
	 *            of station given by met.no
	 * @param latitude
	 * @param longitude
	 * @param currentPosition
	 *            of user for calculation of distance and heading between
	 *            currentPosition and station
	 * @param isReliable
	 *            is true if station delivers data each hour
	 */
	public Station(String name, int id, final double latitude,
			final double longitude, Location currentPosition, boolean isReliable) {
		super();
		this.name = name;
		put(NAME, name);
		this.id = id;
		this.isReliable = isReliable;
		final float[] result = new float[] { distanceToCurrentPosition,
				bearingToCurrentPotition };
		if (currentPosition != null) {
			Location.distanceBetween(currentPosition.getLatitude(),
					currentPosition.getLongitude(), latitude, longitude, result);
			distanceToCurrentPosition = result[0];
			bearingToCurrentPotition = result[1];
			put(DIRECTION, getDirection());
			put(DISTANCE,
					String.format("%.1f km", distanceToCurrentPosition / 1000));
		} else {
			distanceToCurrentPosition = 0;
			bearingToCurrentPotition = 0;
			put(DIRECTION, "");
			put(DISTANCE, "");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Station another) {
		return (int) (distanceToCurrentPosition - another.distanceToCurrentPosition);
	}

	/**
	 * @return Direction from currentPosition to station in degrees
	 */
	public float getBearingFromCurrentPosition() {
		return bearingToCurrentPotition;
	}

	/**
	 * @return Direction from currentPosition to station in text
	 */
	public String getDirection() {
		final float deg = bearingToCurrentPotition;
		if (deg <= 22.5 - 180)
			return "S";
		else if (deg <= 22.5 - 135)
			return "SW";
		else if (deg <= 22.5 - 90)
			return "W";
		else if (deg <= 22.5 - 45)
			return "NW";
		else if (deg <= 22.5)
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

	/**
	 * @return distance between current position and station
	 */
	public float getDistanceToCurrentPosition() {
		return distanceToCurrentPosition;
	}

	/**
	 * @return id used by met.no
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return name of station
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the isReliable
	 */
	public boolean isReliable() {
		return isReliable;
	}

	/**
	 * @param id
	 *            used by met.no
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param isReliable
	 *            the isReliable to set
	 * @param context
	 *            needed for updating of the database
	 */
	public void setIsReliable(boolean isReliable, Context context) {
		final WsKlimaDataBaseHelper db = new WsKlimaDataBaseHelper(context);
		db.setIsReliable(id, isReliable);
		this.isReliable = isReliable;
	}

	/**
	 * @param name
	 *            of station
	 */
	public void setName(String name) {
		this.name = name;
	}

}
