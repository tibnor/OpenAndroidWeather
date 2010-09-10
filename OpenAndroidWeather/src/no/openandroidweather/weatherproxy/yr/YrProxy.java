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
 * 
 */
package no.openandroidweather.weatherproxy.yr;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import no.openandroidweather.misc.GeonamesFindNearbyPlaceNameAsync;
import no.openandroidweather.misc.IProgressItem;
import no.openandroidweather.misc.ProgressWrapper;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weatherproxy.WeatherProxy;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

public class YrProxy implements WeatherProxy {
	private static final String TAG = "YrProxy";
	public static final String PROVIDER = "met.no";
	public static final int AFTER_DOWNLOAD_PROGRESS = 100;
	private final ContentResolver mContentResolver;
	private IProgressItem mProgressItem;
	private final Context mContext;

	public YrProxy(final Context context, final IProgressItem progressItem) {
		super();
		mContentResolver = context.getContentResolver();
		mContext = context;
		mProgressItem = progressItem;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * no.openandroidweather.weatherproxy.WeatherProxy#getWeatherForecast(android
	 * .location.Location, long)
	 */
	@Override
	public Uri getWeatherForecast(final Location location,
			final long lastForecastGenerated, final IProgressItem progressItem)
			throws IOException, Exception {
		mProgressItem = progressItem;
		return locationForecast(location, lastForecastGenerated);
	}

	/**
	 * See http://api.met.no/weatherapi/locationforecast/1.8/documentation for
	 * more information
	 * 
	 * @param location
	 *            of the forecast
	 * @param lastForecastGenerated
	 * @return Uri to the data in WeatherContentProvider
	 * @throws UnknownHostException
	 *             When no Internet connection
	 * @throws SAXException
	 *             parsing exception
	 * @throws ParserConfigurationException
	 * @throws IOException
	 *             Internet trouble
	 */
	private Uri locationForecast(final Location location,
			final long lastForecastGenerated) throws UnknownHostException,
			IOException, ParserConfigurationException, SAXException {
		// Get place:
		final GeonamesFindNearbyPlaceNameAsync placeTask = new GeonamesFindNearbyPlaceNameAsync(
				mContext);
		placeTask.execute(location);

		// Set progress:
		mProgressItem.progress(1);
		// Makes the uri
		final Uri.Builder uri = new Uri.Builder();
		uri.authority("api.met.no");
		uri.path("/weatherapi/locationforecast/1.8/");
		uri.scheme("http");
		final String lat = new Double(location.getLatitude()).toString();
		final String lon = new Double(location.getLongitude()).toString();
		uri.appendQueryParameter("lat", lat);
		uri.appendQueryParameter("lon", lon);
		if (location.hasAltitude()) {
			final Double altD = location.getAltitude();
			final String alt = Integer.toString(altD.intValue());
			uri.appendQueryParameter("msl", alt);
		}
		Log.d(TAG, uri.toString());

		final HttpRequest httpRequest = new HttpGet(uri.toString());
		httpRequest.addHeader("Accept-Encoding", "gzip");

		final HttpClient httpClient = new DefaultHttpClient();
		final HttpHost httpHost = new HttpHost("api.met.no");
		HttpResponse httpResponse = null;
		httpResponse = httpClient.execute(httpHost, httpRequest);

		// Set progress to 10%
		mProgressItem.progress(AFTER_DOWNLOAD_PROGRESS);

		if (httpResponse.getStatusLine().getStatusCode() != 200) {
			final int responseCode = httpResponse.getStatusLine()
					.getStatusCode();
			throw new HttpResponseException(responseCode,
					"Trouble with response from api.yr.no: Response code: "
							+ responseCode);
		}

		final InputStream inputStream = httpResponse.getEntity().getContent();
		final SAXParserFactory spf = SAXParserFactory.newInstance();
		final SAXParser parser = spf.newSAXParser();

		// Parse it
		final YrLocationForecastParser yrLocationForecastParser = new YrLocationForecastParser(
				mContentResolver, lastForecastGenerated, new ProgressWrapper(
						AFTER_DOWNLOAD_PROGRESS, 1000, mProgressItem));
		try {
			parser.parse(inputStream, yrLocationForecastParser);
		} catch (final SAXException e) {
			if (e.getMessage().equals(
					YrLocationForecastParser.NO_NEW_DATA_EXCEPTION)) {
				updateExpectedNewTime(yrLocationForecastParser
						.getNextExcpectedTime());
				return null;
			} else
				throw e;
		}
		mProgressItem.progress(1000);

		final Uri contentUri = yrLocationForecastParser.getContentUri();

		// Wait for geonames
		synchronized (placeTask) {
			if (!placeTask.isDone())
				try {
					placeTask.wait();
				} catch (final InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		final ContentValues values = new ContentValues();
		values.put(WeatherContentProvider.Meta.PLACE_NAME, placeTask.getPlace());
		mContentResolver.update(contentUri, values, null, null);

		return contentUri;
	}

	private void updateExpectedNewTime(final long nextExcpectedTime) {
		final Uri uri = WeatherContentProvider.CONTENT_URI;
		final ContentValues values = new ContentValues();
		values.put(WeatherContentProvider.Meta.NEXT_FORECAST, nextExcpectedTime);
		final String where = WeatherContentProvider.Meta.PROVIDER + "='"
				+ PROVIDER + "'";
		mContentResolver.update(uri, values, where, null);
	}

}
