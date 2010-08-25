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

import java.util.LinkedList;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

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
	public static final String META_NEXT_FORECAST = "new_forecast";
	/**
	 * Time for when the forecast was generated at the server, Integer Unix time
	 * in milliseconds, for meta data
	 */
	public static final String META_GENERATED = "generated";
	/**
	 * Time for when the forecast was loaded to the client. (Downloaded from
	 * server), Integer Unix time in milliseconds, for meta data
	 */
	public static final String META_LOADED = "loaded";
	/** Provider of the forecast */
	public static final String META_PROVIDER = "provider";
	static final String META_CREATE_TABLE = "CREATE TABLE " + META_TABLE_NAME
			+ " (" + META_ID + " INTEGER PRIMARY KEY," + META_ALTITUDE
			+ " REAL," + META_NEXT_FORECAST + " INTEGER," + META_GENERATED
			+ " INTEGER," + META_LATITUDE + " REAL," + META_LOADED
			+ " INTEGER," + META_LONGITUDE + " REAL," + META_PLACE_NAME
			+ " TEXT," + META_PROVIDER + " TEXT)";
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
	/** _id in META table for forecast data, integer */
	static final String FORECAST_META = "meta";
	static final String FORECAST_CREATE_TABLE = "CREATE TABLE "
			+ FORECAST_TABLE_NAME + " (" + FORECAST_ID
			+ " INTEGER PRIMARY KEY, " + FORECAST_FROM + " INTEGER, "
			+ FORECAST_META + " INTEGER, " + FORECAST_TO + " INTEGER, "
			+ FORECAST_TYPE + " INTEGER, " + FORECAST_VALUE + " TEXT )";
	// UriMatcher
	private static final int sMETA = 1;
	private static final int sMETA_ID = 2;

	// /**
	// * Table for adding favorites. When a row is added WeatherService will
	// * update forecasts after the permissions.
	// *
	// */

	// public static class favorite {
	// /**
	// * Id of the favorite, used when asking WeatherService to get a
	// * forecast. Integer
	// */
	// public static final String ID = "_id";
	//
	// /**
	// * A unique TEXT to identify an app in the favorite table
	// */
	// public static final String APPID = "appid";
	// public static final String LATITUDE = "latitude";
	// public static final String LONGITUDE = "longitude";
	// public static final String ALTITUDE = "altitude";
	// public static final String GET_NEAREST = "get_nearest";
	// static final String TABLE_NAME = "favorite";
	// static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
	// + ID + "INTEGER PRIMARY KEY, " + ALTITUDE + " REAL, " + APPID
	// + " TEXT," + GET_NEAREST + " INTEGER, " + LATITUDE + " REAL, "
	// + LONGITUDE + " REAL)";
	// }

	private static final int sFORECAST = 3;
	private static final int sFORECAST_ID = 4;
	private static final int sID_FORECAST = 5;
	private static final int sID_FORECAST_ID = 6;
	private static final UriMatcher sURIMatcher = new UriMatcher(sMETA);

	public WeatherContentProvider() {
		super();

	}

	public WeatherContentProvider(final Context context) {
		super();
		openHelper = new WeatherContentProviderDatabaseOpenHelper(context);
	}

	static {
		final String autority = CONTENT_URI.getAuthority();
		sURIMatcher.addURI(autority, "", sMETA);
		sURIMatcher.addURI(autority, "#", sMETA_ID);
		sURIMatcher.addURI(autority, FORECAST_CONTENT_DIRECTORY, sFORECAST);
		sURIMatcher.addURI(autority, FORECAST_CONTENT_DIRECTORY + "/#",
				sFORECAST_ID);
		sURIMatcher.addURI(autority, "#/" + FORECAST_CONTENT_DIRECTORY,
				sID_FORECAST);
		sURIMatcher.addURI(autority, "#/" + FORECAST_CONTENT_DIRECTORY + "/#",
				sID_FORECAST_ID);
	}

	private WeatherContentProviderDatabaseOpenHelper openHelper;

	@Override
	public int bulkInsert(final Uri uri, final ContentValues[] values) {
		int uriMatch = sURIMatcher.match(uri);
		// TODO: Handle this bug better:
		if (uri.equals(CONTENT_URI))
			uriMatch = 1;

		String table = FORECAST_TABLE_NAME;
		int id = -1;
		switch (uriMatch) {
		case sMETA:
			table = META_TABLE_NAME;
			break;
		case sMETA_ID:
		case sFORECAST_ID:
		case sID_FORECAST_ID:
			throw new UnsupportedOperationException(
					"An insert can not end with an id");
		case sID_FORECAST:
			id = new Integer(uri.getPathSegments().get(0));
			break;
		}

		if (id >= 0)
			for (final ContentValues contentValues : values)
				contentValues.put(FORECAST_META, id);

		int insertedRows = 0;
		final SQLiteDatabase db = openHelper.getWritableDatabase();
		for (final ContentValues contentValues : values)
			if (db.insertOrThrow(table, null, contentValues) != -1)
				insertedRows++;
		db.close();
		return insertedRows;

	}

	/*
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[]) If the URI is for the root level
	 * with a id, all elements connected to this id is also deleted
	 */
	@Override
	public int delete(Uri uri, final String selection,
			final String[] selectionArgs) {
		int uriMatch = sURIMatcher.match(uri);
		// TODO: Handle this bug better:
		if (uri.equals(CONTENT_URI))
			uriMatch = 1;

		final String table = getTable(uri, uriMatch);
		final String newSelection = getNewSelection(uri, selection, uriMatch);

		if (uriMatch == sMETA_ID) {
			uri = Uri.withAppendedPath(uri, FORECAST_CONTENT_DIRECTORY);
			delete(uri, selection, selectionArgs);
		}

		return openHelper.getWritableDatabase().delete(table, newSelection,
				selectionArgs);
	}

	/**
	 * @param uri
	 * @param selection
	 * @param uriMatch
	 * @return
	 */
	private String getNewSelection(final Uri uri, final String selection,
			final int uriMatch) {
		final List<String> where = new LinkedList<String>();
		if (selection != null)
			where.add(selection);
		// Find id in meta table
		switch (uriMatch) {
		case sMETA_ID:
			where.add(META_ID + "=" + uri.getLastPathSegment());
			break;
		case sID_FORECAST:
		case sID_FORECAST_ID:
			where.add(FORECAST_META + "=" + uri.getPathSegments().get(0));

			break;
		}

		// Find id in forecast table
		switch (uriMatch) {
		case sFORECAST_ID:
		case sID_FORECAST_ID:
			where.add(FORECAST_ID + "=" + uri.getLastPathSegment());
		}

		String newSelection = null;
		if (where.size() > 0) {
			newSelection = where.get(0);
			for (int i = 1; i < where.size(); i++)
				newSelection += " AND " + where.get(i);
		}
		return newSelection;
	}

	/**
	 * @param uri
	 * @param uriMatch
	 * @return
	 */
	private String getTable(final Uri uri, final int uriMatch) {
		String table = null;
		switch (uriMatch) {
		case sMETA:
		case sMETA_ID:
			table = META_TABLE_NAME;
			break;
		case sFORECAST:
		case sFORECAST_ID:
		case sID_FORECAST:
		case sID_FORECAST_ID:
			table = FORECAST_TABLE_NAME;
			break;
		default:
			throw new UnsupportedOperationException(
					"Something wrong with the uri!, uriMatch:" + uriMatch
							+ " uri:" + uri.toString());
		}
		return table;
	}

	@Override
	public String getType(final Uri uri) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented!");
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		final String path = uri.getPath();
		String table = null;
		if (path == null || path.equals(""))
			// Is meta data:
			table = META_TABLE_NAME;
		else if (path.contains(FORECAST_CONTENT_DIRECTORY)) {
			// is forecast:
			table = FORECAST_TABLE_NAME;

			// gets id:
			final String split[] = path.split("/");

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

		final SQLiteDatabase db = openHelper.getWritableDatabase();
		final Long id = db.insert(table, null, values);
		db.close();
		return Uri.withAppendedPath(uri, id.toString());
	}

	@Override
	public boolean onCreate() {
		openHelper = new WeatherContentProviderDatabaseOpenHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {
		int uriMatch = sURIMatcher.match(uri);
		// TODO: Handle this bug better:
		if (uri.equals(CONTENT_URI))
			uriMatch = 1;

		final String table = getTable(uri, uriMatch);

		final String newSelection = getNewSelection(uri, selection, uriMatch);

		return openHelper.getReadableDatabase().query(table, projection,
				newSelection, selectionArgs, null, null, sortOrder);

	}

	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		int uriMatch = sURIMatcher.match(uri);
		// TODO: Handle this bug better:
		if (uri.equals(CONTENT_URI))
			uriMatch = 1;

		final String table = getTable(uri, uriMatch);
		final String newSelection = getNewSelection(uri, selection, uriMatch);
		return openHelper.getWritableDatabase().update(table, values,
				newSelection, selectionArgs);
	}

}
