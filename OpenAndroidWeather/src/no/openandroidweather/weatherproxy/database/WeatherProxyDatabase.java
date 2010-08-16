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

package no.openandroidweather.weatherproxy.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class WeatherProxyDatabase {
	private static final String TAG = "WeatherProxyDatabase";

	/**
	 * Table for meta data From each weather forecast there are some meta data,
	 * they are grouped in this table and is referred back with FORECAST_META
	 * which is the row id (META_ID) in this table
	 */
	public static final String META_TABLE_NAME = "meta";
	/** Row id, integer, for meta data */
	public static final String META_ID = "_id";
	/** Longitude, real, for meta data */
	public static final String META_LONGITUDE = "longtitude";
	/** Latitude, real, for meta data */
	public static final String META_LATITUDE = "latitude";
	/** Altitude, real in meter, for meta data */
	public static final String META_ALTITUDE = "altitude";
	/** Name of the place for the forecast, Text, for meta data */
	public static final String META_PLACE_NAME = "place";
	/**
	 * Time for when a new forecast is expected, Integer Unix time in
	 * milliseconds, for meta data
	 */
	public static final String META_EXPIRES = "expires";
	/**
	 * Time for when a the forecast was generated, Integer Unix time in
	 * milliseconds, for meta data
	 */
	public static final String META_GENERATED = "generated";
	static final String META_CREATE_TABLE = "CREATE TABLE " + META_TABLE_NAME
			+ " (" + META_ID + " INTEGER PRIMARY KEY," + META_ALTITUDE
			+ " REAL," + META_EXPIRES + " INTEGER," + META_GENERATED
			+ " INTEGER," + META_LATITUDE + " REAL," + META_LONGITUDE
			+ " REAL," + META_PLACE_NAME + " TEXT)";

	/**
	 * Table for forecast data: This tables have each data point and the meta
	 * data is in the META_TABLE_NAME
	 */
	public static final String FORECAST_TABLE_NAME = "forecast";
	/** Row id in the forecast data table */
	public static final String FORECAST_ID = "_id";

	/**
	 * Time when the valid period for the forecast starts, Integer Unix time in
	 * milliseconds for forecast data
	 */
	public static final String FORECAST_FROM = "fromTime";

	/**
	 * null if the data is a only valid at a specific time or the time when the
	 * valid period for the forecast ends, Integer Unix time in milliseconds for
	 * forecast data
	 */
	public static final String FORECAST_TO = "toTime";
	/** Weather type of forecast data, Integer from WeatherTypes */
	public static final String FORECAST_TYPE = "type";
	/** Value of forecast data, Text */
	public static final String FORECAST_VALUE = "value";
	/** _id in META table for forecast data */
	public static final String FORECAST_META = "meta";
	static final String FORECAST_CREATE_TABLE = "CREATE TABLE "
			+ FORECAST_TABLE_NAME + " (" + FORECAST_ID
			+ " INTEGER PRIMARY KEY, " + FORECAST_FROM + " INTEGER, "
			+ FORECAST_META + " INTEGER, " + FORECAST_TO + " INTEGER, "
			+ FORECAST_TYPE + " INTEGER, " + FORECAST_VALUE + " TEXT )";
	
	private WeatherDatabaseOpenHelper openHelper;
	
	public WeatherProxyDatabase(Context context){
		openHelper = new WeatherDatabaseOpenHelper(context);
	}
}
