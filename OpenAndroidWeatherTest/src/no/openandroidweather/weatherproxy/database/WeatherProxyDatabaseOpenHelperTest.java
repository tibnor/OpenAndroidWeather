package no.openandroidweather.weatherproxy.database;

import org.junit.Before;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

public class WeatherProxyDatabaseOpenHelperTest extends AndroidTestCase {
	WeatherDatabaseOpenHelper openHelper;
	private final static String SCHEMA_V1_META = "CREATE TABLE meta (_id INTEGER PRIMARY KEY,altitude REAL,expires INTEGER,generated INTEGER,latitude REAL,longtitude REAL,place TEXT)";
			final static String SCHEMA_V1_FORECAST = "CREATE TABLE forecast (_id INTEGER PRIMARY KEY, fromTime INTEGER, meta INTEGER, toTime INTEGER, type INTEGER, value TEXT )";

	@Before
	public void setUp() throws Exception {
		Context context = getContext();
		openHelper = new WeatherDatabaseOpenHelper(context);

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

	public void testOnUpgradeSQLiteDatabaseFromv1() {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		openHelper.deleteDatabase(db);
		db.execSQL(SCHEMA_V1_META);
		db.execSQL(SCHEMA_V1_FORECAST);
		addV1TestData(db);
		openHelper.onUpgrade(db, 1, WeatherDatabaseOpenHelper.DATABASE_VERSION);
		checkTestData(db);
	}

	/**
	 * @param db
	 *            Database
	 */
	private void checkTestData(SQLiteDatabase db) {
		Cursor c = db.query(WeatherProxyDatabase.FORECAST_TABLE_NAME, null,
				null, null, null, null, null);
		assertEquals(1, c.getCount());
		c.moveToFirst();
		assertEquals(10000002,
				c.getInt(c.getColumnIndex(WeatherProxyDatabase.FORECAST_FROM)));
		assertEquals(1, c.getInt(c.getColumnIndex(WeatherProxyDatabase.FORECAST_META)));
		assertEquals(10000003,
				c.getInt(c.getColumnIndex(WeatherProxyDatabase.FORECAST_TO)));
		assertEquals(10000004,
				c.getInt(c.getColumnIndex(WeatherProxyDatabase.FORECAST_TYPE)));
		assertEquals("Sun",
				c.getString(c.getColumnIndex(WeatherProxyDatabase.FORECAST_VALUE)));
		c.close();

		c = db.query(WeatherProxyDatabase.META_TABLE_NAME, null, null, null,
				null, null, null);
		assertEquals(1, c.getCount());
		c.moveToFirst();
		assertEquals(123.4,
				c.getDouble(c.getColumnIndex(WeatherProxyDatabase.META_ALTITUDE)));
		assertEquals(10000000,
				c.getInt(c.getColumnIndex(WeatherProxyDatabase.META_EXPIRES)));
		assertEquals(10000001,
				c.getInt(c.getColumnIndex(WeatherProxyDatabase.META_GENERATED)));
		assertEquals(1.1, c.getDouble(c.getColumnIndex(WeatherProxyDatabase.META_LATITUDE)));
		assertEquals(2.2, c.getDouble(c.getColumnIndex(WeatherProxyDatabase.META_LONGITUDE)));
		assertEquals("Norway",
				c.getString(c.getColumnIndex(WeatherProxyDatabase.META_PLACE_NAME)));
		c.close();
	}

	private void addV1TestData(SQLiteDatabase db) {
		ContentValues metaTest = new ContentValues();
		metaTest.put(WeatherProxyDatabase.META_ALTITUDE, 123.4);
		metaTest.put(WeatherProxyDatabase.META_EXPIRES, 10000000);
		metaTest.put(WeatherProxyDatabase.META_GENERATED, 10000001);
		metaTest.put(WeatherProxyDatabase.META_LATITUDE, 1.1);
		metaTest.put(WeatherProxyDatabase.META_LONGITUDE, 2.2);
		metaTest.put(WeatherProxyDatabase.META_PLACE_NAME, "Norway");

		ContentValues forecastTest = new ContentValues();
		forecastTest.put(WeatherProxyDatabase.FORECAST_FROM, 10000002);
		forecastTest.put(WeatherProxyDatabase.FORECAST_META, 1);
		forecastTest.put(WeatherProxyDatabase.FORECAST_TO, 10000003);
		forecastTest.put(WeatherProxyDatabase.FORECAST_TYPE, 10000004);
		forecastTest.put(WeatherProxyDatabase.FORECAST_VALUE, "Sun");

		db.insertOrThrow(WeatherProxyDatabase.FORECAST_TABLE_NAME, null,
				forecastTest);
		db.insertOrThrow(WeatherProxyDatabase.META_TABLE_NAME, null, metaTest);
	}

}
