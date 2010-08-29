package no.openandroidweather.weathercontentprovider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.ProviderTestCase2;

public class WeatherContentProviderTest extends ProviderTestCase2<WeatherContentProvider> {
	public WeatherContentProviderTest(){
		super(WeatherContentProvider.class,WeatherContentProvider.CONTENT_URI.getAuthority());
	}
	
	public WeatherContentProviderTest(
			Class<WeatherContentProvider> providerClass,
			String providerAuthority) {
		super(WeatherContentProvider.class,WeatherContentProvider.CONTENT_URI.getAuthority());
	}



	WeatherContentProvider mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mProvider = new WeatherContentProvider(getContext());
	}

	public void testOnCreate() {
		assertTrue(mProvider.onCreate());
	}

	public void testInsertQuery() {
		// Check Meta:
		mProvider.onCreate();
		ContentValues values = WeatherProxyDatabaseOpenHelperTest
				.metaTestContentValues();
		Uri uri = WeatherContentProvider.CONTENT_URI;
		
		//Returns the uri to the forecast without the meta directory
		uri = mProvider.insert(uri, values);
		Cursor c = mProvider.query(uri, null, null, null, null);
		assertEquals(1, c.getCount());
		WeatherProxyDatabaseOpenHelperTest.checkMetaCursor(c);
		c.close();

		// Check Forecast:
		values = WeatherProxyDatabaseOpenHelperTest
				.forecastTestContentValues();
		uri = Uri.withAppendedPath(uri,
				WeatherContentProvider.Forecast.CONTENT_DIRECTORY);
		
		//Returns the uri to the forecast without the meta directory
		uri = mProvider.insert(uri, values);
		c = mProvider.query(uri, null, null, null, null);
		assertEquals(1, c.getCount());
		WeatherProxyDatabaseOpenHelperTest.checkForecastCursor(c);
		c.close();
	}

	public void testInsertUpdateQuery() {
		fail();
	}

	public void testInsertQueryDeleteQuery() {
		// Check Meta:
		ContentValues values = WeatherProxyDatabaseOpenHelperTest
				.metaTestContentValues();
		Uri uri = WeatherContentProvider.CONTENT_URI;
		
		//Returns the uri to the forecast 
		Uri uriId = mProvider.insert(uri, values);
		Cursor c = mProvider.query(uriId, null, null, null, null);
		assertEquals(1, c.getCount());
		WeatherProxyDatabaseOpenHelperTest.checkMetaCursor(c);
		c.close();

		// Check Forecast:
		values = WeatherProxyDatabaseOpenHelperTest
				.forecastTestContentValues();
		uri = Uri.withAppendedPath(uriId,
				WeatherContentProvider.Forecast.CONTENT_DIRECTORY);
		
		//Returns the uri to the forecast with the forecast directory and id for the forecast
		uri = mProvider.insert(uri, values);
		c = mProvider.query(uri, null, null, null, null);
		assertEquals(1, c.getCount());
		WeatherProxyDatabaseOpenHelperTest.checkForecastCursor(c);
		c.close();
		
		//Gets number of row:
		c = mProvider.query(WeatherContentProvider.CONTENT_URI, null, null, null, null);
		int count = c.getCount();
		c.close();
		
		//Deletes the row and all forecasts:
		assertEquals(1, mProvider.delete(uriId, null, null));
		
		c = mProvider.query(WeatherContentProvider.CONTENT_URI, null, null, null, null);
		assertEquals(count-1, c.getCount());
		c.close();	
		
		//Check that there is no forecast data left:
		uri = Uri.withAppendedPath(uriId, WeatherContentProvider.Forecast.CONTENT_DIRECTORY);
		c = mProvider.query(uri, null,null,null,null);
		
		assertEquals(0, c.getCount());
		c.close();
	}
	
	

	public void testGetType() {
		fail();
	}
}
