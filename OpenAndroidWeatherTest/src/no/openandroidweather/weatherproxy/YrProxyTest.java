package no.openandroidweather.weatherproxy;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import no.openandroidweather.misc.IProgressItem;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.ForecastListView;
import no.openandroidweather.weatherproxy.yr.YrProxy;
import android.content.ContentResolver;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.text.format.Time;
import android.util.Log;

public class YrProxyTest extends ProviderTestCase2<WeatherContentProvider> implements IProgressItem{
	private static final String TAG = "YrProxyTest";

	public YrProxyTest() {
		super(WeatherContentProvider.class, WeatherContentProvider.CONTENT_URI
				.getAuthority());
	}

	public YrProxyTest(Class<WeatherContentProvider> providerClass,
			String providerAuthority) {
		super(WeatherContentProvider.class, WeatherContentProvider.CONTENT_URI
				.getAuthority());
	}

	WeatherProxy yr;
	ContentResolver content;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		content = getContext().getContentResolver();
		yr = new YrProxy(getContext(), this);

	}

	public void testGetWeatherForecast() throws UnknownHostException,
			IOException, Exception {
		// Sets a location Hovedbygget at NTNU, Trondheim as the location
		Double lat = 63.41948, lon = 10.40189, alt = 40.;
		Location loc = new Location("");
		loc.setLatitude(lat);
		loc.setLongitude(lon);
		loc.setAltitude(alt);

		Uri uri = null;
		uri = yr.getWeatherForecast(loc, 0, this);

		Cursor c = content.query(uri, null, null, null, null);
		assertEquals(1, c.getCount());

		c.moveToFirst();
		assertEquals(lat, c.getDouble(c
				.getColumnIndex(WeatherContentProvider.Meta.LATITUDE)), 0.005);
		assertEquals(lon, c.getDouble(c
				.getColumnIndex(WeatherContentProvider.Meta.LONGITUDE)), 0.005);
		assertEquals(alt, c.getDouble(c
				.getColumnIndex(WeatherContentProvider.Meta.ALTITUDE)), 1);
		assertEquals(YrProxy.PROVIDER, c.getString(c
				.getColumnIndex(WeatherContentProvider.Meta.PROVIDER)));
		c.close();

		c = content.query(Uri.withAppendedPath(uri,
				WeatherContentProvider.Forecast.CONTENT_DIRECTORY), null, null,
				null, null);
		assertTrue(500 < c.getCount());
		c.close();
		
		// Checks that there are some data in forecast list view table
		c = content.query(Uri.withAppendedPath(uri, ForecastListView.CONTENT_PATH), null, null, null, null);
		assertTrue(50 < c.getCount());
		c.close();
	}

	public void testGetWeatherForecastOldDate() throws IOException, Exception {
		// Sets a location Hovedbygget at NTNU, Trondheim as the location
		Double lat = 63.41948, lon = 10.40189, alt = 40.;
		Location loc = new Location("");
		loc.setLatitude(lat);
		loc.setLongitude(lon);
		loc.setAltitude(alt);

		assertNull(yr.getWeatherForecast(loc, System.currentTimeMillis(), this));

	}

	public void testGetWeatherForecastNoInternetConnection() {
		fail("Not implemented!");
	}

	
	public void testNewSimpleDateFormat() throws ParseException {
		String timeS = "2010-08-24T12:00:00Z";
		Time time = new Time("UTZ");
		time.parse3339(timeS);
		
		//date.setTimeZone(TimeZone.getTimeZone("UTC"));
		assertEquals(1282651200000l, time.toMillis(false));
	}

	@Override
	public void progress(int progress) {
		Log.d(TAG, "Progress: " + progress);
		
	}
	
	
}
