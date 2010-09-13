package no.openandroidweather.weatherservice;

import java.io.IOException;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import no.openandroidweather.misc.GetLocation;
import no.openandroidweather.misc.IProgressItem;
import no.openandroidweather.misc.WeatherPreferences;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.Forecast;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.Meta;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.Place;
import no.openandroidweather.weatherproxy.WeatherProxy;
import no.openandroidweather.weatherproxy.yr.YrProxy;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
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
	public static final int ERROR_NO_WIFI = 4;
	public static final String TAG = "WeatherService";
	public static final String UPDATE_PLACES = "update places";
	private GetForecast inProgress;
	private static final Queue<GetForecast> mDbCheckQueue = new ConcurrentLinkedQueue<GetForecast>();
	private static final Queue<GetForecast> mDownloadQueue = new ConcurrentLinkedQueue<GetForecast>();
	private static final Set<Location> mDownloadingForecasts = new HashSet<Location>();
	private static Boolean isWorking = false;
	private static boolean isUpdatingForecastForPlaces = false;
	private static PendingIntent mAlarmIntent = null;
	public final static double defaultAceptanceRadius = 2000.;
	public final static double defaultAceptanceVerticalDistance = 100.;

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

			Location loc = GetLocation.getLocation(WeatherService.this);
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
	 * Check if the forecast exist in meta table and next forecast is in the
	 * future
	 * 
	 * @param metaId
	 * @return
	 */
	private boolean checkIfForecastIsUpdated(int metaId) {
		String[] projection = { Meta.NEXT_FORECAST };
		Uri uri = Uri.withAppendedPath(WeatherContentProvider.CONTENT_URI,
				metaId + "");
		Cursor c = getContentResolver()
				.query(uri, projection, null, null, null);

		if (c.getCount() != 1) {
			c.close();
			return false;
		}

		c.moveToFirst();
		long nextForecast = c.getLong(c
				.getColumnIndexOrThrow(Meta.NEXT_FORECAST));

		if (nextForecast >= System.currentTimeMillis()) {
			c.close();
			return true;
		} else {
			c.close();
			return false;
		}
	};

	/**
	 * Check if there are any forecasts in database, and calls the event
	 * listener if it is in the db. If data is old or not found in db, the
	 * forecast is downloaded
	 * 
	 * @param EventListener
	 *            to check
	 * @throws RemoteException
	 */
	public void checkInDbOrDownload(final GetForecast getForecast) {
		Log.i(TAG, "Start  Id:" + WeatherService.this.hashCode());

		inProgress = getForecast;
		final ContentResolver cr = getContentResolver();
		final String sortOrder = Meta.GENERATED + " DESC";
		// Gets all forecasts
		final Cursor c = cr.query(WeatherContentProvider.CONTENT_URI, null,
				null, null, sortOrder);

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
					downloadForcast(getForecast);
				} else {
					getForecast.completed();
					jobCompleted();
				}
				c.close();
				return;
			}
		}
		c.close();
		downloadForcast(getForecast);
		jobCompleted();

	}

	/**
	 * Deletes forecasts who has a newer one within the tolerance distance or
	 * with no forecasts for the future.
	 */
	public void deleteOldForecasts() {
		final ContentResolver cr = getContentResolver();
		// Check if there are some forecast who has a newer one
		final String sortOrder = Meta.GENERATED + " DESC";
		final Cursor c = cr.query(WeatherContentProvider.CONTENT_URI, null,
				null, null, sortOrder);

		Set<Location> locationWithForecast = new HashSet<Location>();
		c.moveToFirst();
		int length = c.getCount();
		int latCol = c.getColumnIndexOrThrow(Meta.LATITUDE);
		int lonCol = c.getColumnIndexOrThrow(Meta.LONGITUDE);
		int altCol = c.getColumnIndexOrThrow(Meta.ALTITUDE);
		int idCol = c.getColumnIndexOrThrow(Meta._ID);
		for (int i = 0; i < length; i++) {
			// Get place of forecast
			Location rowLocation = new Location("");
			rowLocation.setAltitude(c.getDouble(altCol));
			rowLocation.setLatitude(c.getDouble(latCol));
			rowLocation.setLongitude(c.getDouble(lonCol));
			int id = c.getInt(idCol);
			// Check if this radius is within tolerance radius of any other
			boolean deleteForecast = false;
			for (Location location : locationWithForecast) {
				if (location.distanceTo(rowLocation) < defaultAceptanceRadius
						&& (Math.abs(location.getAltitude()
								- rowLocation.getAltitude()) < defaultAceptanceVerticalDistance || rowLocation
								.getAltitude() == 0)) {
					deleteForecast = true;
					break;
				}
			}
			if (deleteForecast || !hasForecastInFuture(id)) {
				Uri uri = Uri.withAppendedPath(
						WeatherContentProvider.CONTENT_URI, id + "");
				getContentResolver().delete(uri, null, null);
			} else {
				locationWithForecast.add(rowLocation);
			}
			c.moveToNext();
		}
		c.close();

	};

	/**
	 * Check if the forecast has any sub forecasts in the future or if all
	 * forecast is in the past
	 * 
	 * @param id
	 *            in meta table.
	 * @return
	 */
	private boolean hasForecastInFuture(int id) {
		Uri uri = Uri.withAppendedPath(WeatherContentProvider.CONTENT_URI, id
				+ "");
		Uri forecastUri = Uri.withAppendedPath(uri, Forecast.CONTENT_DIRECTORY);
		String[] projection = { Forecast._ID };
		String selection = Forecast.FROM + ">" + System.currentTimeMillis();
		Cursor c = getContentResolver().query(forecastUri, projection,
				selection, null, null);
		int length = c.getCount();
		c.close();

		return length > 0;
	}

	/**
	 * Downloads forecast from Internet, and calls the event listener when
	 * completed. If it is already downloading it is added to mDbCheckQueue
	 * 
	 * @param getForecast
	 *            to check
	 * @throws RemoteException
	 */
	void downloadForcast(final GetForecast getForecast) {
		// Check if it has wifi connection if required.
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		Boolean downloadOnlyOnWifi = preferences.getBoolean(
				WeatherPreferences.DOWNLOAD_ONLY_ON_WIFI,
				WeatherPreferences.DOWNLOAD_ONLY_ON_WIFI_DEFAULT);
		if (downloadOnlyOnWifi) {
			ConnectivityManager connManager = (ConnectivityManager) this
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			Boolean onWifi = connManager.getNetworkInfo(
					ConnectivityManager.TYPE_WIFI).isConnected();
			if (!onWifi) {
				getForecast.exceptionOccured(ERROR_NO_WIFI);
				return;
			}
		}

		synchronized (mDownloadingForecasts) {
			for (Location downloadingLocation : mDownloadingForecasts) {
				if (getForecast.isInTargetZone(
						downloadingLocation.getLatitude(),
						downloadingLocation.getLongitude(),
						downloadingLocation.getAltitude())) {
					mDbCheckQueue.add(getForecast);
					return;
				}
			}

			mDownloadingForecasts.add(getForecast.getLocation());
		}

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

		// Update number of downloaded forecasts
		int downloadedforecasts = preferences.getInt(
				WeatherPreferences.NUMBER_OF_DOWNLOADED_FORECASTS,
				WeatherPreferences.NUMBER_OF_DOWNLOADED_FORECASTS_DEFAULT);
		downloadedforecasts++;
		
		SharedPreferences.Editor edit = preferences.edit();
		edit.putInt(WeatherPreferences.NUMBER_OF_DOWNLOADED_FORECASTS, downloadedforecasts);
		edit.commit();

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
		synchronized (mDownloadingForecasts) {
			if (!mDownloadingForecasts.remove(getForecast)) {
				Log.e(TAG, "Did not remove note of downloading");
				mDownloadingForecasts.clear();
			}
		}

		getForecast.completed();
		jobCompleted();
	}

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
	public void onCreate() {
		super.onCreate();
		setAlarm();
	}

	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		Log.d(TAG, "Id:" + this.hashCode());

		Bundle extra = intent.getExtras();
		if (extra != null) {
			boolean updatePlaces = extra.getBoolean(UPDATE_PLACES);
			if (updatePlaces) {
				new Thread(new Runnable() {
					public void run() {
						updateForecastForPlaces();
					}
				}).start();
			}
		}
	}

	@Override
	public void progress(int progress) {
		progress += numberOfJobsCompleted * 1000;
		if (numberOfJobsStarted > 0)
			progress /= numberOfJobsStarted;

		for (final GetForecast i : mDbCheckQueue)
			i.progress(progress);

		for (final GetForecast i : mDownloadQueue)
			i.progress(progress);

		if (inProgress != null)
			inProgress.progress(progress);

	}

	public void setAlarm() {
		String[] projection = { Meta.NEXT_FORECAST };
		String sortOrder = Meta.NEXT_FORECAST + " DESC";
		Cursor c = getContentResolver().query(
				WeatherContentProvider.CONTENT_URI, projection, null, null,
				sortOrder);

		long nextForecast = 0;
		if (c.getCount() > 0) {
			c.moveToFirst();
			nextForecast = c.getLong(c
					.getColumnIndexOrThrow(Meta.NEXT_FORECAST));
		}
		c.close();

		if (nextForecast < System.currentTimeMillis()) {
			// Try again next hour
			nextForecast = System.currentTimeMillis() + 1000 * 3600;
		}

		if (mAlarmIntent == null) {

			Intent intent = new Intent(this, WeatherService.class);
			intent.putExtra(UPDATE_PLACES, true);

			mAlarmIntent = PendingIntent.getService(this, 0, intent,
					PendingIntent.FLAG_CANCEL_CURRENT);
		}

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		alarmManager.set(AlarmManager.RTC, nextForecast, mAlarmIntent);

	}

	/**
	 * Check if all places has a forecast that is updated, if not download a new
	 * one. At the end delete all forecast who has a newer forecast and forecast
	 * who has no sub-forecast for the future.
	 */
	private void updateForecastForPlaces() {
		if (isUpdatingForecastForPlaces) {
			Log.i(TAG,
					"Updating of forecasts for places is already ongoing, request ignored");
			return;
		} else {
			Log.i(TAG, "Starting updating forecasts for places");
			isUpdatingForecastForPlaces = true;
		}

		Cursor c = getContentResolver().query(Place.CONTENT_URI, null, null,
				null, null);

		int length = c.getCount();
		int forecastCol = c.getColumnIndexOrThrow(Place.FORECAST_ROW);
		int longtitudeCol = c.getColumnIndexOrThrow(Place.LONGITUDE);
		int latitudeCol = c.getColumnIndexOrThrow(Place.LATITUDE);
		int altitudeCol = c.getColumnIndexOrThrow(Place.ALTITUDE);
		int idCol = c.getColumnIndexOrThrow(Place._ID);
		c.moveToFirst();
		for (int i = 0; i < length; i++) {
			if (!checkIfForecastIsUpdated(c.getInt(forecastCol))) {
				// Adds place to db check
				Location location = new Location("");
				location.setLatitude(c.getDouble(latitudeCol));
				location.setLongitude(c.getDouble(longtitudeCol));
				location.setAltitude(c.getDouble(altitudeCol));
				PlaceForecastEventListener listener = new PlaceForecastEventListener(
						c.getInt(idCol));
				GetForecast getForecast = new GetForecast(listener, location,
						defaultAceptanceRadius,
						defaultAceptanceVerticalDistance);
				checkInDbOrDownload(getForecast);
			}
			c.moveToNext();
		}
		c.close();

		deleteOldForecasts();

		setAlarm();

		// If one of the queues is not empty the are started again.
		if (!mDbCheckQueue.isEmpty() && !mDownloadQueue.isEmpty())
			work();

		Log.v(TAG, "Updating of forecasts is completed");
		// Toast.makeText(this, "Updating of forecasts is completed",
		// Toast.LENGTH_SHORT);
		isUpdatingForecastForPlaces = false;
	}

	protected void work() {
		if (!isWorking) {
			isWorking = true;
			new WorkAsync().execute(new Void[] { null });
		}

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
					&& (Math.abs(alt - location.getAltitude()) > toleranceVertical)
					&& location.getAltitude() != 0)
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

	private class PlaceForecastEventListener implements IForecastEventListener {
		int placeId;

		public PlaceForecastEventListener(int placeId) {
			super();
			this.placeId = placeId;
		}

		@Override
		public IBinder asBinder() {
			return null;
		}

		@Override
		public void completed() throws RemoteException {
		}

		@Override
		public void exceptionOccurred(int errorcode) throws RemoteException {
		}

		@Override
		public void newExpectedTime() throws RemoteException {
		}

		@Override
		public void newForecast(String uri, long forecastGenerated)
				throws RemoteException {
			int forecastId = new Integer(Uri.parse(uri).getLastPathSegment());
			ContentValues values = new ContentValues();
			values.put(Place.FORECAST_ROW, forecastId);
			getContentResolver().update(
					Uri.withAppendedPath(Place.CONTENT_URI, placeId + ""),
					values, null, null);
		}

		@Override
		public void progress(int progress) throws RemoteException {
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
			// Check db and download if the forecast is not updated or not in db
			Log.d(TAG, "Checking database, " + mDbCheckQueue.size()
					+ " in queue");
			while (!mDbCheckQueue.isEmpty())
				checkInDbOrDownload(mDbCheckQueue.poll());
			// Download from Internet
			Log.d(TAG, "Downloading, " + mDownloadQueue.size()
					+ " in queue, service:" + WeatherService.this.hashCode());
			while (!mDownloadQueue.isEmpty())
				downloadForcast(mDownloadQueue.poll());

			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			super.onPostExecute(result);
			if (mDownloadQueue.isEmpty() && mDbCheckQueue.isEmpty()) {
				isWorking = false;
				numberOfJobsCompleted = numberOfJobsStarted;
				progress(0);
				stopSelf();
			} else
				new WorkAsync().execute(new Void[] { null });
		}

	}
}
