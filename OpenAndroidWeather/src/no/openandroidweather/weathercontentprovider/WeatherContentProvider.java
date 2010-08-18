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

package no.openandroidweather.weathercontentprovider;

import java.net.URISyntaxException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;
import android.widget.SectionIndexer;

/**
 * The WeatherProxy should call the provider when inserting, the WeatherService
 * to check witch forecast is available and the client to get the forecast
 * 
 */
public class WeatherContentProvider extends ContentProvider {
	private static final String TAG = "WeatherContentProvider";

	public static final Uri CONTENT_URI = Uri
			.parse("content://no.openandroidweather.weathercontentprovider");
	public static final String FORECAST_CONTENT_DIRECTORY = "forecast";
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
	/** Provider of the forecast */
	public static final String META_PROVIDER = "provider";
	static final String META_CREATE_TABLE = "CREATE TABLE " + META_TABLE_NAME
			+ " (" + META_ID + " INTEGER PRIMARY KEY," + META_ALTITUDE
			+ " REAL," + META_EXPIRES + " INTEGER," + META_GENERATED
			+ " INTEGER," + META_LATITUDE + " REAL," + META_LONGITUDE
			+ " REAL," + META_PLACE_NAME + " TEXT," + META_PROVIDER + " TEXT)";

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

	// UriMatcher
	private static final int sMETA = 1;
	private static final int sMETA_ID = 2;
	private static final int sFORECAST = 3;
	private static final int sFORECAST_ID = 4;
	private static final int sID_FORECAST = 5;
	private static final int sID_FORECAST_ID = 6;
	private static final UriMatcher sURIMatcher = new UriMatcher(sMETA);

	static {
		String autority = CONTENT_URI.getAuthority();
		sURIMatcher.addURI(autority, "#", sMETA_ID);
		sURIMatcher.addURI(autority, FORECAST_CONTENT_DIRECTORY, sFORECAST);
		sURIMatcher.addURI(autority, FORECAST_CONTENT_DIRECTORY + "/#", sFORECAST_ID);
		sURIMatcher.addURI(autority, "#/" + FORECAST_CONTENT_DIRECTORY, sID_FORECAST);
		sURIMatcher.addURI(autority, "#/" + FORECAST_CONTENT_DIRECTORY + "/#",
				sID_FORECAST_ID);
	}

	private WeatherContentProviderDatabaseOpenHelper openHelper;

	public WeatherContentProvider(Context context) {
		openHelper = new WeatherContentProviderDatabaseOpenHelper(context);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String path = uri.getPath();
		String table = null;
		if (path == null || path.equals("")) {
			// Is meta data:
			table = META_TABLE_NAME;
		} else if (path.contains(FORECAST_CONTENT_DIRECTORY)) {
			// is forecast:
			table = FORECAST_TABLE_NAME;

			// gets id:
			String split[] = path.split("/");

			// Checks that the second split is a integer
			if (!split[1].matches("^-{0,1}[0-9]+$"))
				throw new UnsupportedOperationException(
						"Path was not recognized in insert:" + path);
			values.put(FORECAST_META, split[1]);

		} else {
			Log.e(TAG, "Path was not recognized in insert:" + path);
			throw new UnsupportedOperationException(
					"Path was not recognized in insert:" + path);
		}

		SQLiteDatabase db = openHelper.getWritableDatabase();
		Long id = db.insert(table, null, values);
		db.close();
		return Uri.withAppendedPath(uri, id.toString());
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder q = new SQLiteQueryBuilder();

		// Find table
		int uriMatch = sURIMatcher.match(uri);
		Log.d(TAG, new Integer(uriMatch).toString()+uri);
		switch (uriMatch) {
		case sMETA:
		case sMETA_ID:
			q.setTables(META_TABLE_NAME);
			break;
		case sFORECAST:
		case sFORECAST_ID:
		case sID_FORECAST:
		case sID_FORECAST_ID:
			q.setTables(FORECAST_TABLE_NAME);
			break;
		default:
			Log.e(TAG, "Something wrong with the uri!, uriMatch:"+uriMatch+" uri:"+uri.toString());
			throw new UnsupportedOperationException("Something wrong with the uri!");
		}

		//Find id in meta table
		switch (uriMatch) {
		case sMETA_ID:
			q.appendWhereEscapeString(META_ID + "=" + uri.getLastPathSegment());
			break;
		case sID_FORECAST:
		case sID_FORECAST_ID:
			q.appendWhereEscapeString(FORECAST_META + "="
					+ uri.getPathSegments().get(0));

			break;
		}

		//Find id in forecast table
		switch (uriMatch) {
		case sFORECAST_ID:
		case sID_FORECAST_ID:
			q.appendWhereEscapeString(FORECAST_ID + "="
					+ uri.getLastPathSegment());
		}

		
		return q.query(openHelper.getReadableDatabase(), projection, selection,
				selectionArgs, null, null, sortOrder);

	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented!");
	}

}
