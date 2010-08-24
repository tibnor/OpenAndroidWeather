package no.openandroidweather.weatherservice;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weatherproxy.WeatherProxy;
import no.openandroidweather.weatherproxy.YrProxy;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

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

/**
 * This service should get the forecast from yr.no, via event listeners.
 */
// Tricks from this guide:
// http://developer.android.com/guide/developing/tools/aidl.html#exposingtheinterface
public class WeatherService extends Service {
	public final static int ERROR_NETWORK_ERROR = 1;
	public final static int ERROR_UNKNOWN_ERROR = 2;
	public static final int ERROR_NO_KNOWN_POSITION = 3;
	public static final String TAG = "WeatherService";
	private Queue<GetForecast> mDbCheckQueue = new ConcurrentLinkedQueue<GetForecast>();
	private Queue<GetForecast> mDownloadQueue = new ConcurrentLinkedQueue<GetForecast>();
	private Boolean isWorking = false;

	@Override
	public IBinder onBind(Intent intent) {
		if (IWeatherService.class.getName().equals(intent.getAction()))
			return mBind;

		return mBind;
	}

	private final IWeatherService.Stub mBind = new IWeatherService.Stub() {
		@Override
		public void getForecast(IForecastEventListener listener,
				double latitude, double longitude, double altitude,
				double toleranceRadius, double toleranceVerticalDistance)
				throws RemoteException {
			Location loc = new Location("");
			loc.setLatitude(latitude);
			loc.setLongitude(longitude);
			if (altitude != 0) {
				loc.setAltitude(altitude);
			}
			mDbCheckQueue.add(new GetForecast(listener, loc, toleranceRadius,
					toleranceVerticalDistance));
			work();
		}

		@Override
		public void getNearestForecast(IForecastEventListener listener,
				double toleranceRadius, double toleranceVerticalDistance)
				throws RemoteException {
			LocationManager locationManager = (LocationManager) WeatherService.this
					.getApplicationContext().getSystemService(
							Context.LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			String provider = locationManager.getBestProvider(criteria, true);
			if(provider==null){
				listener.exceptionOccurred(ERROR_NO_KNOWN_POSITION);
				return;
			}
			
			Location loc = locationManager.getLastKnownLocation(provider);
			if (loc == null) {
				listener.exceptionOccurred(ERROR_NO_KNOWN_POSITION);
				return;
			}
			GetForecast getForecast = new GetForecast(listener, loc,
					toleranceRadius, toleranceVerticalDistance);
			mDbCheckQueue.add(getForecast);
			work();
		}
	};
	
	public void onDestroy() {
		super.onDestroy();
	};

	/**
	 * Gets forecast needs to be started from the UI thread.
	 */
	private class WorkAsync extends AsyncTask<Void, Float, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			// Check db
			Log.d(TAG, "Checking database, " + mDbCheckQueue.size()
					+ " in queue");
			while (!mDbCheckQueue.isEmpty()) {
				checkInDb(mDbCheckQueue.poll());

			}
			// Download from Internet
			Log.d(TAG, "Downloading, " + mDownloadQueue.size() + " in queue");
			while (!mDownloadQueue.isEmpty()) {
				downloadForcast(mDownloadQueue.poll());
			}

			// Delete all old forecasts:
			deleteOldForecasts();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			//synchronized (isWorking) {
				if (mDownloadQueue.isEmpty() && mDbCheckQueue.isEmpty()) {
					isWorking = false;
					stopSelf();
				} else
					new WorkAsync().execute(null);
		//	}

		}

	}

	protected void work() {
		//synchronized (isWorking) {
			if (!isWorking) {
				isWorking = true;
				new WorkAsync().execute(null);
			}
		//}
	}

	public void deleteOldForecasts() {
		ContentResolver cr = getContentResolver();
		// Check what is the latest forecast:

		String[] projection = { WeatherContentProvider.META_GENERATED };
		String selection = WeatherContentProvider.META_PROVIDER + "='"
				+ YrProxy.PROVIDER + "'";
		String sortOrder = WeatherContentProvider.META_GENERATED + " DESC";
		Cursor c = cr.query(WeatherContentProvider.CONTENT_URI, projection,
				selection, null, sortOrder);

		// Check that there is forecasts in the table
		if (c.getCount() == 0)
			return;
		c.moveToFirst();
		long latestGeneratedForecast = c.getLong(c
				.getColumnIndexOrThrow(WeatherContentProvider.META_GENERATED));
		c.close();

		// Deletes all old forecasts:
		String where = WeatherContentProvider.META_GENERATED + "<"
				+ latestGeneratedForecast + " AND " + selection;
		cr.delete(WeatherContentProvider.CONTENT_URI, where, null);
	}

	/**
	 * Check if there are any forecasts in database, and calls the event
	 * listener if it is in the db. If data is old or not found in db, the
	 * listener is added to the download queue.
	 * 
	 * @param EventListener
	 *            to check
	 * @throws RemoteException
	 */
	public void checkInDb(GetForecast getForecast) {
		ContentResolver cr = getContentResolver();
		// Gets all forecasts
		Cursor c = cr.query(WeatherContentProvider.CONTENT_URI, null, null,
				null, null);

		// Gets columns
		int latCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.META_LATITUDE);
		int lonCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.META_LONGITUDE);
		int altCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.META_ALTITUDE);
		double lat, lon, alt;

		// Goes thru all forecasts
		int length = c.getCount();
		for (int i = 0; i < length; i++) {
			c.moveToPosition(i);
			lat = c.getDouble(latCol);
			lon = c.getDouble(lonCol);
			alt = c.getDouble(altCol);
			if (getForecast.isInTargetZone(lat, lon, alt)) {
				// Sends a message to listener
				Integer id = c.getInt(c
						.getColumnIndexOrThrow(WeatherContentProvider.META_ID));

				long generated = c.getLong(c
						.getColumnIndex(WeatherContentProvider.META_GENERATED));

				getForecast.newForecast(
						Uri.withAppendedPath(
								WeatherContentProvider.CONTENT_URI,
								id.toString()).toString(), generated);
				// Notify that there is a new forecast
				//synchronized (getForecast.getListener()) {
				//	getForecast.getListener().notifyAll();
				//}

				// Check if it is expected a new forecast:
				long expectedNextForecast = c
						.getLong(c
								.getColumnIndexOrThrow(WeatherContentProvider.META_NEXT_FORECAST));
				long now = System.currentTimeMillis();
				if (now > expectedNextForecast) {
					long lastGeneratedForecast = c
							.getLong(c
									.getColumnIndexOrThrow(WeatherContentProvider.META_GENERATED));
					getForecast.setLastGeneratedForecast(lastGeneratedForecast);
					mDownloadQueue.add(getForecast);
				} else {
					getForecast.completed();
				}

				c.close();
				return;
			}
		}

		mDownloadQueue.add(getForecast);
		c.close();

	}

	/**
	 * Downloads forecast from Internet, and calls the event listener when
	 * completed.
	 * 
	 * @param getForecast
	 *            to check
	 * @throws RemoteException
	 */
	void downloadForcast(GetForecast getForecast) {
		WeatherProxy proxy = new YrProxy(getContentResolver());
		Uri uri = null;
		try {
			uri = proxy.getWeatherForecast(getForecast.getLocation(),
					getForecast.getLastGeneratedForecast());
		} catch (IOException e) {
			getForecast.exceptionOccured(ERROR_NETWORK_ERROR);
		} catch (Exception e) {
			getForecast.exceptionOccured(ERROR_UNKNOWN_ERROR);
		}

		// If no new forecast send status update and delete downloads
		// queue.
		if (uri == null) {
			for (GetForecast l : mDownloadQueue) {
				l.newExpectedTime();
			}
			mDownloadQueue.clear();
		} else {
			String uriS = uri.toString();

			Cursor c = getContentResolver().query(uri, null, null, null, null);
			c.moveToFirst();
			long generated = c.getLong(c
					.getColumnIndex(WeatherContentProvider.META_GENERATED));
			c.close();

			getForecast.newForecast(uriS, generated);
		}

		// Notify that there is a new forecast
		//synchronized (getForecast.getListener()) {
		//	getForecast.getListener().notifyAll();
		//}
		getForecast.completed();

	}

	class GetForecast {
		public GetForecast(IForecastEventListener listener, Location location,
				double toleranceRadius, double toleranceVertical) {
			super();
			this.listener = listener;
			this.location = location;
			this.toleranceRadius = toleranceRadius;
			this.toleranceVertical = toleranceVertical;
		}

		public void completed() {
			try {
				listener.completed();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public Location getLocation() {
			return location;
		}

		final private IForecastEventListener listener;
		final private Location location;
		final private double toleranceRadius;
		final private double toleranceVertical;
		private long lastGeneratedForecast;

		public void newExpectedTime() {
			try {
				listener.newExpectedTime();
			} catch (RemoteException e) {
				// If message is not received do nothing
				Log.e(TAG, "newExpectedTime" + e.getMessage());
			}
		}

		public void exceptionOccured(int errorcode) {
			try {
				listener.exceptionOccurred(errorcode);
			} catch (RemoteException e) {
				// If message is not received do nothing
				Log.e(TAG, "newExpectedTime" + e.getMessage());
			}
		}

		public long getLastGeneratedForecast() {
			return lastGeneratedForecast;
		}

		public void newForecast(String uri, long generated) {
			try {
				listener.newForecast(uri, generated);
			} catch (RemoteException e) {
				// If message is not received do nothing
				Log.e(TAG, "newExpectedTime" + e.getMessage());
			}
		}

		public boolean isInTargetZone(double lat, double lon, double alt) {
			if (location.hasAltitude()
					&& (Math.abs(alt - location.getAltitude()) > toleranceVertical))
				return false;
			float dist[] = new float[3];
			Location.distanceBetween(lat, lon, location.getLatitude(),
					location.getLongitude(), dist);
			if (dist[0] > toleranceRadius)
				return false;
			return true;
		}

		public IForecastEventListener getListener() {
			return listener;
		}

		public void setLastGeneratedForecast(long lastGeneratedForecast) {
			this.lastGeneratedForecast = lastGeneratedForecast;
		}

		public double getToleranceRadius() {
			return toleranceRadius;
		}

		public double getToleranceVertical() {
			return toleranceVertical;
		}
	}

}
