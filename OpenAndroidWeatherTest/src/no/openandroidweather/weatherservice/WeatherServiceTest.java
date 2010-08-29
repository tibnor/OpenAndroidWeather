package no.openandroidweather.weatherservice;

import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.ServiceTestCase;
import android.util.Log;

public class WeatherServiceTest extends ServiceTestCase<WeatherService> {

	private static final double latitude = 60;
	private static final double longitude = 11;
	private static final double altitude = 200;
	private static final double toleranceRadius = 10000;
	private static final double toleranceVerticalDistance = 50;
	public static final String TAG = "WeatherServiceTest";
	private IWeatherService mService;

	public WeatherServiceTest() {
		super(WeatherService.class);
	}

	public WeatherServiceTest(Class<WeatherService> serviceClass) {
		super(WeatherService.class);
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IWeatherService.Stub.asInterface(service);

		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

	};

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IBinder binder = bindService(new Intent(IWeatherService.class.getName()));
		mServiceConnection.onServiceConnected(null, binder);
		
		ContentResolver cr = getContext().getContentResolver();
		cr.delete(WeatherContentProvider.CONTENT_URI, null, null);
	}

	public void testGetForecast() throws RemoteException,
			InterruptedException {
		TestForecastEventListener listener = new TestForecastEventListener();
		synchronized (listener) {
			mService.getForecast(listener, latitude, longitude, altitude,
					toleranceRadius, toleranceVerticalDistance);
			listener.wait();
		}
		String uri = listener.getUri();
		assertNotNull(uri);
		
		//Check that it is cached
		synchronized (listener) {
			mService.getForecast(listener, latitude, longitude, altitude,
					toleranceRadius, toleranceVerticalDistance);
			listener.wait(1000);
		}
		uri = listener.getUri();
		assertNotNull(uri);
	}

	public void testGetNearestForecast() throws RemoteException,
			InterruptedException {
		TestForecastEventListener listener = new TestForecastEventListener();
		synchronized (listener) {
			mService.getNearestForecast(listener, 2000, 50);
			listener.wait();
		}
		String uri = listener.getUri();
		assertNotNull(uri);
		
		synchronized (listener) {
			mService.getNearestForecast(listener, 2000, 50);
			listener.wait(1000);
		}
		uri = listener.getUri();
		assertNotNull(uri);
	}

	/*
	 * public void testCheckDb() throws InterruptedException { ContentResolver
	 * cr = getContext().getContentResolver(); ContentValues values = new
	 * ContentValues(); values.put(key, value)
	 * cr.insert(WeatherContentProvider.CONTENT_URI, values );
	 * 
	 * 
	 * //Gets the face forecast WeatherService weather = getService();
	 * TestForecastEventListener listener = new TestForecastEventListener();
	 * GetForecast getForecast = weather.new GetForecast(listener, latitude,
	 * longitude, altitude, toleranceRadius, toleranceVerticalDistance);
	 * 
	 * synchronized (listener) { weather.checkInDb(getForecast);
	 * listener.wait(1000); } String uri = listener.getUri();
	 * assertNotNull(uri); }
	 */

	private class TestForecastEventListener extends IForecastEventListener.Stub {

		private String uri;

		@Override
		public void newForecast(String uri, long forecastGenerated)
				throws RemoteException {
			this.setUri(uri);
			Log.i(TAG, "new forecast");
		}

		@Override
		public void progress(int progress) throws RemoteException {
			Log.i(TAG, "new progree");

		}

		@Override
		public void newExpectedTime() throws RemoteException {
			Log.i(TAG, "new expected time");

		}

		@Override
		public void exceptionOccurred(int errorcode) throws RemoteException {
			throw new UnknownError("Error code:" + errorcode);

		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public String getUri() {
			return uri;
		}

		@Override
		public void completed() throws RemoteException {
			// TODO Auto-generated method stub
			
		}

	}

}
