package no.openandroidweather.weathercontentprovider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

public class WeatherProxyDatabaseOpenHelperTest extends AndroidTestCase {
	WeatherContentProviderDatabaseOpenHelper openHelper;
	private final static String SCHEMA_V1_META = "CREATE TABLE meta (_id INTEGER PRIMARY KEY,altitude REAL,expires INTEGER,generated INTEGER,latitude REAL,longtitude REAL,place TEXT,provider TEXT)";
			final static String SCHEMA_V1_FORECAST = "CREATE TABLE forecast (_id INTEGER PRIMARY KEY, fromTime INTEGER, meta INTEGER, toTime INTEGER, type INTEGER, value TEXT )";
			


	public void setUp() throws Exception {
		Context context = getContext();
		openHelper = new WeatherContentProviderDatabaseOpenHelper(context);

	}

	public void testOnCreateSQLiteDatabase() {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		openHelper.deleteDatabase(db);
		openHelper.onCreate(db);
		db.close();
		
		db = openHelper.getWritableDatabase();
		addTestData(db);
		checkTestData(db);
		db.close();
	}

	private void addTestData(SQLiteDatabase db) {
		addV1TestData(db);
	}

	/*
	public void testOnUpgradeSQLiteDatabaseFromv1() {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		openHelper.deleteDatabase(db);
		db.execSQL(SCHEMA_V1_META);
		db.execSQL(SCHEMA_V1_FORECAST);
		addV1TestData(db);
		openHelper.onUpgrade(db, 1, WeatherContentProviderDatabaseOpenHelper.DATABASE_VERSION);
		checkTestData(db);
	}
*/

	/**
	 * @param db
	 *            Database
	 */
	private void checkTestData(SQLiteDatabase db) {
		Cursor c = db.query(WeatherContentProvider.FORECAST_TABLE_NAME, null,
				null, null, null, null, null);
		assertEquals(1, c.getCount());
		checkForecastCursor(c);
		assertEquals(1, c.getInt(c.getColumnIndex(WeatherContentProvider.FORECAST_META)));

		c.close();

		c = db.query(WeatherContentProvider.META_TABLE_NAME, null, null, null,
				null, null, null);
		assertEquals(1, c.getCount());
		checkMetaCursor(c);
		c.close();
	}

	/**
	 * @param c
	 */
	static void checkMetaCursor(Cursor c) {
		c.moveToFirst();
		assertEquals(123.4,
				c.getDouble(c.getColumnIndex(WeatherContentProvider.META_ALTITUDE)));
		assertEquals(10000000,
				c.getInt(c.getColumnIndex(WeatherContentProvider.META_NEXT_FORECAST)));
		assertEquals(10000001,
				c.getInt(c.getColumnIndex(WeatherContentProvider.META_GENERATED)));
		assertEquals(1.1, c.getDouble(c.getColumnIndex(WeatherContentProvider.META_LATITUDE)));
		assertEquals(2.2, c.getDouble(c.getColumnIndex(WeatherContentProvider.META_LONGITUDE)));
		assertEquals("Norway",
				c.getString(c.getColumnIndex(WeatherContentProvider.META_PLACE_NAME)));
		assertEquals("yr.no",
				c.getString(c.getColumnIndex(WeatherContentProvider.META_PROVIDER)));
		assertEquals(123456, c.getInt(c.getColumnIndexOrThrow(WeatherContentProvider.META_LOADED)));
	}

	/**
	 * @param c
	 */
	static void checkForecastCursor(Cursor c) {
		c.moveToFirst();
		assertEquals(10000002,
				c.getInt(c.getColumnIndex(WeatherContentProvider.FORECAST_FROM)));
		assertEquals(10000003,
				c.getInt(c.getColumnIndex(WeatherContentProvider.FORECAST_TO)));
		assertEquals(10000004,
				c.getInt(c.getColumnIndex(WeatherContentProvider.FORECAST_TYPE)));
		assertEquals("Sun",
				c.getString(c.getColumnIndex(WeatherContentProvider.FORECAST_VALUE)));
	}

	private void addV1TestData(SQLiteDatabase db) {
		ContentValues metaTest = metaTestContentValues();

		ContentValues forecastTest = forecastTestContentValues();

		db.insertOrThrow(WeatherContentProvider.FORECAST_TABLE_NAME, null,
				forecastTest);
		db.insertOrThrow(WeatherContentProvider.META_TABLE_NAME, null, metaTest);
	}

	/**
	 * @return
	 */
	static ContentValues forecastTestContentValues() {
		ContentValues forecastTest = new ContentValues();
		forecastTest.put(WeatherContentProvider.FORECAST_FROM, 10000002);
		forecastTest.put(WeatherContentProvider.FORECAST_META, 1);
		forecastTest.put(WeatherContentProvider.FORECAST_TO, 10000003);
		forecastTest.put(WeatherContentProvider.FORECAST_TYPE, 10000004);
		forecastTest.put(WeatherContentProvider.FORECAST_VALUE, "Sun");
		return forecastTest;
	}

	/**
	 * @return a set of metaTestContentValues
	 */
	static ContentValues metaTestContentValues() {
		ContentValues metaTest = new ContentValues();
		metaTest.put(WeatherContentProvider.META_LOADED, 123456);
		metaTest.put(WeatherContentProvider.META_ALTITUDE, 123.4);
		metaTest.put(WeatherContentProvider.META_NEXT_FORECAST, 10000000);
		metaTest.put(WeatherContentProvider.META_GENERATED, 10000001);
		metaTest.put(WeatherContentProvider.META_LATITUDE, 1.1);
		metaTest.put(WeatherContentProvider.META_LONGITUDE, 2.2);
		metaTest.put(WeatherContentProvider.META_PLACE_NAME, "Norway");
		metaTest.put(WeatherContentProvider.META_PROVIDER, "yr.no");
		return metaTest;
	}
	


}
