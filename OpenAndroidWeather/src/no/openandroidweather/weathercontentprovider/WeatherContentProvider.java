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

/**
 * The WeatherProxy should call the provider when inserting, the WeatherService
 * to check witch forecast is available and the client to get the forecast
 * 
 */
public class WeatherContentProvider extends ContentProvider {
	@SuppressWarnings("unused")
	private static final String TAG = "WeatherContentProvider";

	public static final Uri CONTENT_URI = Uri
			.parse("content://no.openandroidweather.weathercontentprovider");

	// UriMatcher
	private static final int sMETA = 1;
	private static final int sMETA_ID = 2;
	private static final int sFORECAST = 3;
	private static final int sFORECAST_ID = 4;
	private static final int sID_FORECAST = 5;
	private static final int sID_FORECAST_ID = 6;
	private static final int sID_FORECAST_LIST_VIEW = 7;
	private static final int sID_FORECAST_LIST_VIEW_ID = 8;
	private static final int sPLACE = 9;
	private static final int sPLACE_ID = 10;
	private static final UriMatcher sURIMatcher = new UriMatcher(sMETA);

	public WeatherContentProvider() {
		super();

	}

	public WeatherContentProvider(final Context context) {
		super();
		openHelper = new WeatherContentProviderDatabaseOpenHelper(context);
	}

	static {
		final String authority = CONTENT_URI.getAuthority();
		sURIMatcher.addURI(authority, "", sMETA);
		sURIMatcher.addURI(authority, "#", sMETA_ID);
		sURIMatcher.addURI(authority, Forecast.CONTENT_DIRECTORY, sFORECAST);
		sURIMatcher.addURI(authority, Forecast.CONTENT_DIRECTORY + "/#",
				sFORECAST_ID);
		sURIMatcher.addURI(authority, "#/" + Forecast.CONTENT_DIRECTORY,
				sID_FORECAST);
		sURIMatcher.addURI(authority, "#/" + Forecast.CONTENT_DIRECTORY + "/#",
				sID_FORECAST_ID);
		sURIMatcher.addURI(authority, "#/" + ForecastListView.CONTENT_PATH,
				sID_FORECAST_LIST_VIEW);
		sURIMatcher.addURI(authority, "#/" + ForecastListView.CONTENT_PATH
				+ "/#", sID_FORECAST_LIST_VIEW_ID);
		sURIMatcher.addURI(authority, Place.CONTENT_PATH, sPLACE);
		sURIMatcher.addURI(authority, Place.PLACE_NAME + "/#", sPLACE_ID);
	}

	private WeatherContentProviderDatabaseOpenHelper openHelper;

	/**
	 * @param values
	 * @param path
	 */
	private ContentValues addIdToContentValues(final ContentValues values,
			final int uriMatch, final Uri uri) {
		switch (uriMatch) {
		case sMETA:
		case sFORECAST:
		case sPLACE:
			break;
		case sID_FORECAST:
			values.put(Forecast.META, uri.getPathSegments().get(0));
			break;
		case sID_FORECAST_LIST_VIEW:
			values.put(ForecastListView.metaId, uri.getPathSegments().get(0));
			break;
		case sMETA_ID:
		case sFORECAST_ID:
		case sID_FORECAST_ID:
		case sID_FORECAST_LIST_VIEW_ID:
		case sPLACE_ID:
			throw new UnsupportedOperationException("Can not insert into a row");
		default:
			throw new UnsupportedOperationException(
					"Path was not recognized in insert:");
		}
		return values;
	}

	@Override
	public int bulkInsert(final Uri uri, final ContentValues[] values) {
		final int uriMatch = getUriMatch(uri);

		final String table = getTable(uri, uriMatch);

		for (ContentValues contentValues : values)
			contentValues = addIdToContentValues(contentValues, uriMatch, uri);

		int insertedRows = 0;
		final SQLiteDatabase db = openHelper.getWritableDatabase();
		for (final ContentValues contentValues : values)
			if (db.insertOrThrow(table, null, contentValues) != -1)
				insertedRows++;
		return insertedRows;

	}

	/*
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[]) If the URI is for the root level
	 * with a id, all elements connected to this id is also deleted
	 */
	@Override
	public int delete(final Uri uri, final String selection,
			final String[] selectionArgs) {
		final int uriMatch = getUriMatch(uri);

		final String table = getTable(uri, uriMatch);
		final String newSelection = getNewSelection(uri, selection, uriMatch);

		if (uriMatch == sMETA_ID) {
			final Uri forecastUri = Uri.withAppendedPath(uri,
					Forecast.CONTENT_DIRECTORY);
			delete(forecastUri, selection, selectionArgs);
			final Uri listViewUri = Uri.withAppendedPath(uri,
					ForecastListView.CONTENT_PATH);
			delete(listViewUri, selection, selectionArgs);

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
			where.add(Meta._ID + "=" + uri.getLastPathSegment());
			break;
		case sID_FORECAST:
		case sID_FORECAST_ID:
			where.add(Forecast.META + "=" + uri.getPathSegments().get(0));
			break;
		case sID_FORECAST_LIST_VIEW_ID:
			where.add(ForecastListView._id + "=" + uri.getLastPathSegment());
		case sID_FORECAST_LIST_VIEW:
			where.add(ForecastListView.metaId + "="
					+ uri.getPathSegments().get(0));
			break;
		case sPLACE_ID:
			where.add(Place._ID + "=" + uri.getLastPathSegment());
		}

		// Find id in forecast table
		switch (uriMatch) {
		case sFORECAST_ID:
		case sID_FORECAST_ID:
			where.add(Forecast._ID + "=" + uri.getLastPathSegment());
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
			table = Meta.TABLE_NAME;
			break;
		case sFORECAST:
		case sFORECAST_ID:
		case sID_FORECAST:
		case sID_FORECAST_ID:
			table = Forecast.TABLE_NAME;
			break;
		case sID_FORECAST_LIST_VIEW_ID:
		case sID_FORECAST_LIST_VIEW:
			table = ForecastListView.TABLE_NAME;
			break;
		case sPLACE:
		case sPLACE_ID:
			table = Place.TABLE_NAME;
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

	/**
	 * @param uri
	 * @return
	 */
	private int getUriMatch(final Uri uri) {
		int uriMatch = sURIMatcher.match(uri);
		// TODO: Handle this bug better:
		if (uri.equals(CONTENT_URI))
			uriMatch = 1;
		return uriMatch;
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		final int uriMatch = getUriMatch(uri);
		final String table = getTable(uri, uriMatch);
		addIdToContentValues(values, uriMatch, uri);

		final SQLiteDatabase db = openHelper.getWritableDatabase();
		final Long id = db.insert(table, null, values);
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
		final int uriMatch = getUriMatch(uri);

		final String table = getTable(uri, uriMatch);
		final String newSelection = getNewSelection(uri, selection, uriMatch);

		return openHelper.getReadableDatabase().query(table, projection,
				newSelection, selectionArgs, null, null, sortOrder);

	}

	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		final int uriMatch = getUriMatch(uri);

		final String table = getTable(uri, uriMatch);
		final String newSelection = getNewSelection(uri, selection, uriMatch);
		return openHelper.getWritableDatabase().update(table, values,
				newSelection, selectionArgs);
	}

	/**
	 * Table for forecast data: This tables have each data point and the meta
	 * data is in the META_TABLE_NAME
	 */
	public static class Forecast {
		public static final String CONTENT_DIRECTORY = "forecast";
		public static final String TABLE_NAME = "forecast";
		/** Row id in the forecast data table */
		public static final String _ID = "_id";

		/**
		 * Time when the valid period for the forecast starts, Integer Unix time
		 * in milliseconds for forecast data
		 */
		public static final String FROM = "fromTime";
		/**
		 * null if the data is a only valid at a specific time or the time when
		 * the valid period for the forecast ends, Integer Unix time in
		 * milliseconds for forecast data
		 */
		public static final String TO = "toTime";

		/** Weather type of forecast data, Integer from WeatherTypes */
		public static final String TYPE = "type";

		/** Value of forecast data, Text */
		public static final String VALUE = "value";
		/** _id in META table for forecast data, integer */
		static final String META = "meta";
		static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
				+ _ID + " INTEGER PRIMARY KEY, " + FROM + " INTEGER, " + META
				+ " INTEGER, " + TO + " INTEGER, " + TYPE + " INTEGER, "
				+ VALUE + " TEXT )";
	}

	/**
	 * Table for data for list view
	 */
	public static class ForecastListView {
		static final String TABLE_NAME = "forecastListView";
		static final String metaId = "metaId";
		public final static String CONTENT_PATH = "forecastListView";
		/**
		 * Row id, integer
		 */
		public static final String _id = "_id";
		/**
		 * Start time of the forecast, integer in ms since Unix epoch time
		 */
		public static final String fromTime = "fromTime";
		/**
		 * End time of the forecast, integer in ms since Unix epoch time
		 */
		public static final String toTime = "toTime";
		/**
		 * Temperature at the start time in degree of Celcius
		 */
		public static final String temperature = "temperature";
		/**
		 * Weather symbol thru out the time span
		 */
		public static final String symbol = "symbol";
		/**
		 * Precipitation during the time span in mm
		 */
		public static final String percipitation = "percipitation";
		/**
		 * Wind speed at start time in m/s
		 */
		public static final String windSpeed = "windSpeed";
		/**
		 * Wind direction at start time in degrees (360)
		 */
		public static final String windDirection = "windDirection";

		static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
				+ _id + " INTEGER PRIMARY KEY, " + metaId + " INTEGER, "
				+ fromTime + " INTEGER, " + toTime + " INTEGER, " + temperature
				+ " REAL," + symbol + " INTEGER," + percipitation + " REAL, "
				+ windSpeed + " REAL," + windDirection + " REAL)";

	}

	/**
	 * Table for meta data From each weather forecast there are some meta data,
	 * they are grouped in this table and is referred back with FORECAST_META
	 * which is the row id (META_ID) in this table
	 */
	public static class Meta {
		public static final String TABLE_NAME = "meta";
		/** Row id, integer, for meta data */
		public static final String _ID = "_id";
		/** Longitude, real, for meta data */
		public static final String LONGITUDE = "longtitude";
		/** Latitude, real, for meta data */
		public static final String LATITUDE = "latitude";
		/** Altitude, real in meter, for meta data */
		public static final String ALTITUDE = "altitude";
		/** Name of the place for the forecast, Text, for meta data */
		public static final String PLACE_NAME = "place";
		/**
		 * Time for when a new forecast is expected, Integer Unix time in
		 * milliseconds, for meta data
		 */
		public static final String NEXT_FORECAST = "new_forecast";
		/**
		 * Time for when the forecast was generated at the server, Integer Unix
		 * time in milliseconds, for meta data
		 */
		public static final String GENERATED = "generated";
		/**
		 * Time for when the forecast was loaded to the client. (Downloaded from
		 * server), Integer Unix time in milliseconds, for meta data
		 */
		public static final String LOADED = "loaded";
		/** Provider of the forecast */
		public static final String PROVIDER = "provider";
		static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
				+ _ID + " INTEGER PRIMARY KEY," + ALTITUDE + " REAL,"
				+ NEXT_FORECAST + " INTEGER," + GENERATED + " INTEGER,"
				+ LATITUDE + " REAL," + LOADED + " INTEGER," + LONGITUDE
				+ " REAL," + PLACE_NAME + " TEXT," + PROVIDER + " TEXT)";
	}

	/**
	 * Table for storing places.
	 * 
	 */
	public static class Place {
		public static final String CONTENT_PATH = "places";

		public static final String TABLE_NAME = "places";
		/** Row id, integer, for place data */
		public static final String _ID = "_id";
		/** Longitude, real, for place data */
		public static final String LONGITUDE = "longtitude";
		/** Latitude, real, for place data */
		public static final String LATITUDE = "latitude";
		/** Altitude, real in meter, for place data */
		public static final String ALTITUDE = "altitude";
		/** Name of the place for the forecast, Text, for place data */
		public static final String PLACE_NAME = "place";

		static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
				+ _ID + " INTEGER PRIMARY KEY," + ALTITUDE + " REAL,"
				+ LATITUDE + " REAL," + LONGITUDE + " REAL," + PLACE_NAME
				+ " TEXT)";
	}
}
