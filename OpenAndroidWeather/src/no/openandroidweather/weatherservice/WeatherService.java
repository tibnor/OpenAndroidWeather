package no.openandroidweather.weatherservice;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import no.openandroidweather.misc.IProgressItem;
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
public class WeatherService extends Service implements IProgressItem {
	public final static int ERROR_NETWORK_ERROR = 1;
	public final static int ERROR_UNKNOWN_ERROR = 2;
	public static final int ERROR_NO_KNOWN_POSITION = 3;
	public static final String TAG = "WeatherService";
	private GetForecast inProgress;
	private final Queue<GetForecast> mDbCheckQueue = new ConcurrentLinkedQueue<GetForecast>();
	private final Queue<GetForecast> mDownloadQueue = new ConcurrentLinkedQueue<GetForecast>();
	private Boolean isWorking = false;

	/**
	 * Used in progress, for each GetForecast in mDbCheckQueue there is 2 jobs
	 * (db check and download). for each GetForecast in mDownloadQueue there is
	 * 1 job. Is calculated at beginning of a new async job
	 */
	private int numberOfJobsStarted = 0;
	private int numberOfJobsCompleted = 0;

	private final IWeatherService.Stub mBind = new IWeatherService.Stub() {
		@Override
		public void getForecast(final IForecastEventListener listener,
				final double latitude, final double longitude,
				final double altitude, final double toleranceRadius,
				final double toleranceVerticalDistance) throws RemoteException {
			final Location loc = new Location("");
			loc.setLatitude(latitude);
			loc.setLongitude(longitude);
			if (altitude != 0)
				loc.setAltitude(altitude);
			mDbCheckQueue.add(new GetForecast(listener, loc, toleranceRadius,
					toleranceVerticalDistance));
			work();
		}

		@Override
		public void getNearestForecast(final IForecastEventListener listener,
				final double toleranceRadius,
				final double toleranceVerticalDistance) throws RemoteException {
			final LocationManager locationManager = (LocationManager) WeatherService.this
					.getApplicationContext().getSystemService(
							Context.LOCATION_SERVICE);
			final Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			final String provider = locationManager.getBestProvider(criteria,
					true);
			if (provider == null) {
				listener.exceptionOccurred(ERROR_NO_KNOWN_POSITION);
				return;
			}

			final Location loc = locationManager.getLastKnownLocation(provider);
			if (loc == null) {
				listener.exceptionOccurred(ERROR_NO_KNOWN_POSITION);
				return;
			}
			final GetForecast getForecast = new GetForecast(listener, loc,
					toleranceRadius, toleranceVerticalDistance);
			mDbCheckQueue.add(getForecast);
			work();
		}
	};

	/**
	 * Check if there are any forecasts in database, and calls the event
	 * listener if it is in the db. If data is old or not found in db, the
	 * listener is added to the download queue.
	 * 
	 * @param EventListener
	 *            to check
	 * @throws RemoteException
	 */
	public void checkInDb(final GetForecast getForecast) {
		inProgress = getForecast;
		final ContentResolver cr = getContentResolver();
		// Gets all forecasts
		final Cursor c = cr.query(WeatherContentProvider.CONTENT_URI, null,
				null, null, null);

		// Gets columns
		final int latCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.Meta.LATITUDE);
		final int lonCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.Meta.LONGITUDE);
		final int altCol = c
				.getColumnIndexOrThrow(WeatherContentProvider.Meta.ALTITUDE);
		double lat, lon, alt;

		// Goes thru all forecasts
		final int length = c.getCount();
		for (int i = 0; i < length; i++) {
			c.moveToPosition(i);
			lat = c.getDouble(latCol);
			lon = c.getDouble(lonCol);
			alt = c.getDouble(altCol);
			if (getForecast.isInTargetZone(lat, lon, alt)) {
				// Sends a message to listener
				final Integer id = c
						.getInt(c
								.getColumnIndexOrThrow(WeatherContentProvider.Meta._ID));

				final long generated = c.getLong(c
						.getColumnIndex(WeatherContentProvider.Meta.GENERATED));

				getForecast.newForecast(
						Uri.withAppendedPath(
								WeatherContentProvider.CONTENT_URI,
								id.toString()).toString(), generated);
				// Notify that there is a new forecast
				// synchronized (getForecast.getListener()) {
				// getForecast.getListener().notifyAll();
				// }

				// Check if it is expected a new forecast:
				final long expectedNextForecast = c
						.getLong(c
								.getColumnIndexOrThrow(WeatherContentProvider.Meta.NEXT_FORECAST));
				final long now = System.currentTimeMillis();
				if (now > expectedNextForecast) {
					final long lastGeneratedForecast = c
							.getLong(c
									.getColumnIndexOrThrow(WeatherContentProvider.Meta.GENERATED));
					getForecast.setLastGeneratedForecast(lastGeneratedForecast);
					mDownloadQueue.add(getForecast);
				} else {
					getForecast.completed();
					jobCompleted();
				}
				c.close();
				return;
			}
		}
		c.close();
		mDownloadQueue.add(getForecast);
		jobCompleted();

	}

	public void deleteOldForecasts() {
		final ContentResolver cr = getContentResolver();
		// Check what is the latest forecast:

		final String[] projection = { WeatherContentProvider.Meta.GENERATED };
		final String selection = WeatherContentProvider.Meta.PROVIDER + "='"
				+ YrProxy.PROVIDER + "'";
		final String sortOrder = WeatherContentProvider.Meta.GENERATED
				+ " DESC";
		final Cursor c = cr.query(WeatherContentProvider.CONTENT_URI,
				projection, selection, null, sortOrder);

		// Check that there is forecasts in the table
		if (c.getCount() == 0)
			return;
		c.moveToFirst();
		final long latestGeneratedForecast = c.getLong(c
				.getColumnIndexOrThrow(WeatherContentProvider.Meta.GENERATED));
		c.close();

		// Deletes all old forecasts:
		final String where = WeatherContentProvider.Meta.GENERATED + "<"
				+ latestGeneratedForecast + " AND " + selection;
		cr.delete(WeatherContentProvider.CONTENT_URI, where, null);
	}

	/**
	 * Downloads forecast from Internet, and calls the event listener when
	 * completed.
	 * 
	 * @param getForecast
	 *            to check
	 * @throws RemoteException
	 */
	void downloadForcast(final GetForecast getForecast) {
		inProgress = getForecast;
		final WeatherProxy proxy = new YrProxy(getApplicationContext(), this);
		Uri uri = null;
		try {
			uri = proxy.getWeatherForecast(getForecast.getLocation(),
					getForecast.getLastGeneratedForecast(), this);
		} catch (final IOException e) {
			getForecast.exceptionOccured(ERROR_NETWORK_ERROR);
		} catch (final Exception e) {
			getForecast.exceptionOccured(ERROR_UNKNOWN_ERROR);
		}

		// If no new forecast send status update and delete downloads
		// queue.
		if (uri == null) {
			for (final GetForecast l : mDownloadQueue)
				l.newExpectedTime();
			mDownloadQueue.clear();
		} else {
			final String uriS = uri.toString();

			final Cursor c = getContentResolver().query(uri, null, null, null,
					null);
			c.moveToFirst();
			final long generated = c.getLong(c
					.getColumnIndex(WeatherContentProvider.Meta.GENERATED));
			c.close();

			getForecast.newForecast(uriS, generated);
		}

		// Notify that there is a new forecast
		// synchronized (getForecast.getListener()) {
		// getForecast.getListener().notifyAll();
		// }
		getForecast.completed();
		jobCompleted();
	};

	/**
	 * 
	 */
	private void jobCompleted() {
		numberOfJobsCompleted++;
		progress(0);
	}

	@Override
	public IBinder onBind(final Intent intent) {
		if (IWeatherService.class.getName().equals(intent.getAction()))
			return mBind;

		return mBind;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void progress(int progress) {
		progress += numberOfJobsCompleted * 1000;
		progress /= numberOfJobsStarted;

		for (final GetForecast i : mDbCheckQueue)
			i.progress(progress);

		for (final GetForecast i : mDownloadQueue)
			i.progress(progress);

		if (inProgress != null)
			inProgress.progress(progress);

	}

	protected void work() {
		// synchronized (isWorking) {
		if (!isWorking) {
			isWorking = true;
			new WorkAsync().execute(new Void[] {null});
		}
		// }
	}

	class GetForecast {
		final private IForecastEventListener listener;

		final private Location location;

		final private double toleranceRadius;

		final private double toleranceVertical;
		private long lastGeneratedForecast;

		public GetForecast(final IForecastEventListener listener,
				final Location location, final double toleranceRadius,
				final double toleranceVertical) {
			super();
			this.listener = listener;
			this.location = location;
			this.toleranceRadius = toleranceRadius;
			this.toleranceVertical = toleranceVertical;
		}

		public void completed() {
			try {
				listener.completed();
			} catch (final RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void exceptionOccured(final int errorcode) {
			try {
				listener.exceptionOccurred(errorcode);
			} catch (final RemoteException e) {
				// If message is not received do nothing
				Log.e(TAG, "newExpectedTime" + e.getMessage());
			}
		}

		public long getLastGeneratedForecast() {
			return lastGeneratedForecast;
		}

		public IForecastEventListener getListener() {
			return listener;
		}

		public Location getLocation() {
			return location;
		}

		public double getToleranceRadius() {
			return toleranceRadius;
		}

		public double getToleranceVertical() {
			return toleranceVertical;
		}

		public boolean isInTargetZone(final double lat, final double lon,
				final double alt) {
			if (location.hasAltitude()
					&& (Math.abs(alt - location.getAltitude()) > toleranceVertical))
				return false;
			final float dist[] = new float[3];
			Location.distanceBetween(lat, lon, location.getLatitude(),
					location.getLongitude(), dist);
			if (dist[0] > toleranceRadius)
				return false;
			return true;
		}

		public void newExpectedTime() {
			try {
				listener.newExpectedTime();
			} catch (final RemoteException e) {
				// If message is not received do nothing
				Log.e(TAG, "newExpectedTime" + e.getMessage());
			}
		}

		public void newForecast(final String uri, final long generated) {
			try {
				listener.newForecast(uri, generated);
			} catch (final RemoteException e) {
				// If message is not received do nothing
				Log.e(TAG, "newExpectedTime" + e.getMessage());
			}
		}

		public void progress(final int progress) {
			try {
				listener.progress(progress);
			} catch (final RemoteException e) {
				Log.e(TAG, "newExpectedTime" + e.getMessage());
			}
		}

		public void setLastGeneratedForecast(final long lastGeneratedForecast) {
			this.lastGeneratedForecast = lastGeneratedForecast;
		}
	}

	/**
	 * Gets forecast needs to be started from the UI thread.
	 */
	private class WorkAsync extends AsyncTask<Void, Float, Void> {
		@Override
		protected Void doInBackground(final Void... params) {
			numberOfJobsStarted = mDbCheckQueue.size() * 2
					+ mDownloadQueue.size();
			numberOfJobsCompleted = 0;
			// Check db
			Log.d(TAG, "Checking database, " + mDbCheckQueue.size()
					+ " in queue");
			while (!mDbCheckQueue.isEmpty())
				checkInDb(mDbCheckQueue.poll());
			// Download from Internet
			Log.d(TAG, "Downloading, " + mDownloadQueue.size() + " in queue");
			while (!mDownloadQueue.isEmpty())
				downloadForcast(mDownloadQueue.poll());

			// Delete all old forecasts:
			deleteOldForecasts();

			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			super.onPostExecute(result);
			// synchronized (isWorking) {
			if (mDownloadQueue.isEmpty() && mDbCheckQueue.isEmpty()) {
				isWorking = false;
				numberOfJobsCompleted = numberOfJobsStarted;
				progress(0);
				stopSelf();
			} else
				new WorkAsync().execute(new Void[] {null});
			// }

		}

	}
}
