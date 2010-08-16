package no.openandroidweather.weatherproxy.database;

import junit.framework.TestCase;

import org.powermock.api.easymock.PowerMock;

import android.content.Context;

public class WeatherProxyDatabaseOpenHelperTest extends TestCase {
	WeatherDatabaseOpenHelper openHelper;

	protected void setUp() throws Exception {
		super.setUp();
		Context context = PowerMock.createMock(Context.class);
		openHelper = new WeatherDatabaseOpenHelper(context);
	}

	public void testOpenHelper() {
		fail("Not yet implemented"); // TODO
	}

	public void testOnCreate() {
		fail("Not yet implemented"); // TODO
	}

	public void testOnUpgradeSQLiteDatabaseIntInt() {
		fail("Not yet implemented"); // TODO
	}

	public void testGetWritableDatabase() {
		fail("Not yet implemented"); // TODO
	}

	public void testGetReadableDatabase() {
		fail("Not yet implemented"); // TODO
	}

	public void testClose() {
		fail("Not yet implemented"); // TODO
	}

}
