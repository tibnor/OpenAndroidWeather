package no.openandroidweather.weatherproxy;

import java.io.IOException;

import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import android.content.ContentResolver;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.test.ProviderTestCase2;

public class YrProxyTest extends ProviderTestCase2<WeatherContentProvider> {
	public YrProxyTest() {
		super(WeatherContentProvider.class,WeatherContentProvider.CONTENT_URI.getAuthority());
	}
	
	public YrProxyTest(Class<WeatherContentProvider> providerClass,
			String providerAuthority) {
		super(WeatherContentProvider.class,WeatherContentProvider.CONTENT_URI.getAuthority());
	}

	WeatherProxy yr;
	ContentResolver content;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		content = getContext().getContentResolver();
		yr = new YrProxy(content);

	}

	public void testGetWeatherForecast() throws IOException, Exception {
		// Sets a location Hovedbygget at NTNU, Trondheim as the location
		Double lat = 63.41948, lon = 10.40189, alt = 40.;
		Location loc = new Location("");
		loc.setLatitude(lat);
		loc.setLongitude(lon);
		loc.setAltitude(alt);

		Uri uri = null;
		uri = yr.getWeatherForecast(loc);

		Cursor c = content.query(uri, null, null, null, null);
		assertEquals(1, c.getCount());

		c.moveToFirst();
		assertEquals(lat, c.getDouble(c
				.getColumnIndex(WeatherContentProvider.META_LATITUDE)),0.005);
		assertEquals(lon, c.getDouble(c
				.getColumnIndex(WeatherContentProvider.META_LONGITUDE)),0.005);
		assertEquals(alt, c.getDouble(c
				.getColumnIndex(WeatherContentProvider.META_ALTITUDE)),1);
		assertEquals(YrProxy.PROVIDER, c.getString(c.getColumnIndex(WeatherContentProvider.META_PROVIDER)));
		c.close();
		
		c = content.query(Uri.withAppendedPath(uri,WeatherContentProvider.FORECAST_CONTENT_DIRECTORY), null, null, null, null);
		assertTrue(500<c.getCount());
		c.close();
	}
}
