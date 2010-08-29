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
		Cursor c = db.query(WeatherContentProvider.Forecast.TABLE_NAME, null,
				null, null, null, null, null);
		assertEquals(1, c.getCount());
		checkForecastCursor(c);
		assertEquals(1, c.getInt(c.getColumnIndex(WeatherContentProvider.Forecast.META)));

		c.close();

		c = db.query(WeatherContentProvider.Meta.TABLE_NAME, null, null, null,
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
				c.getDouble(c.getColumnIndex(WeatherContentProvider.Meta.ALTITUDE)));
		assertEquals(10000000,
				c.getInt(c.getColumnIndex(WeatherContentProvider.Meta.NEXT_FORECAST)));
		assertEquals(10000001,
				c.getInt(c.getColumnIndex(WeatherContentProvider.Meta.GENERATED)));
		assertEquals(1.1, c.getDouble(c.getColumnIndex(WeatherContentProvider.Meta.LATITUDE)));
		assertEquals(2.2, c.getDouble(c.getColumnIndex(WeatherContentProvider.Meta.LONGITUDE)));
		assertEquals("Norway",
				c.getString(c.getColumnIndex(WeatherContentProvider.Meta.PLACE_NAME)));
		assertEquals("yr.no",
				c.getString(c.getColumnIndex(WeatherContentProvider.Meta.PROVIDER)));
		assertEquals(123456, c.getInt(c.getColumnIndexOrThrow(WeatherContentProvider.Meta.LOADED)));
	}

	/**
	 * @param c
	 */
	static void checkForecastCursor(Cursor c) {
		c.moveToFirst();
		assertEquals(10000002,
				c.getInt(c.getColumnIndex(WeatherContentProvider.Forecast.FROM)));
		assertEquals(10000003,
				c.getInt(c.getColumnIndex(WeatherContentProvider.Forecast.TO)));
		assertEquals(10000004,
				c.getInt(c.getColumnIndex(WeatherContentProvider.Forecast.TYPE)));
		assertEquals("Sun",
				c.getString(c.getColumnIndex(WeatherContentProvider.Forecast.VALUE)));
	}

	private void addV1TestData(SQLiteDatabase db) {
		ContentValues metaTest = metaTestContentValues();

		ContentValues forecastTest = forecastTestContentValues();

		db.insertOrThrow(WeatherContentProvider.Forecast.TABLE_NAME, null,
				forecastTest);
		db.insertOrThrow(WeatherContentProvider.Meta.TABLE_NAME, null, metaTest);
	}

	/**
	 * @return
	 */
	static ContentValues forecastTestContentValues() {
		ContentValues forecastTest = new ContentValues();
		forecastTest.put(WeatherContentProvider.Forecast.FROM, 10000002);
		forecastTest.put(WeatherContentProvider.Forecast.META, 1);
		forecastTest.put(WeatherContentProvider.Forecast.TO, 10000003);
		forecastTest.put(WeatherContentProvider.Forecast.TYPE, 10000004);
		forecastTest.put(WeatherContentProvider.Forecast.VALUE, "Sun");
		return forecastTest;
	}

	/**
	 * @return a set of metaTestContentValues
	 */
	static ContentValues metaTestContentValues() {
		ContentValues metaTest = new ContentValues();
		metaTest.put(WeatherContentProvider.Meta.LOADED, 123456);
		metaTest.put(WeatherContentProvider.Meta.ALTITUDE, 123.4);
		metaTest.put(WeatherContentProvider.Meta.NEXT_FORECAST, 10000000);
		metaTest.put(WeatherContentProvider.Meta.GENERATED, 10000001);
		metaTest.put(WeatherContentProvider.Meta.LATITUDE, 1.1);
		metaTest.put(WeatherContentProvider.Meta.LONGITUDE, 2.2);
		metaTest.put(WeatherContentProvider.Meta.PLACE_NAME, "Norway");
		metaTest.put(WeatherContentProvider.Meta.PROVIDER, "yr.no");
		return metaTest;
	}
	


}
