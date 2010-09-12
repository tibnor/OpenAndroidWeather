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

package no.openandroidweather.misc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import no.openandroidweather.R;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

public class Geonames {
	private static final String TAG = "Geonames";
	private final Context mContext;

	public Geonames(final Context context) {
		mContext = context;
	}

	public String findNearestPlace(final Location location)
			throws ClientProtocolException, IOException,
			ParserConfigurationException, SAXException {
		// Makes the uri
		final Uri.Builder uri = new Uri.Builder();
		uri.authority("ws.geonames.org");
		uri.path("findNearbyPlaceName");
		uri.scheme("http");
		final String lat = new Double(location.getLatitude()).toString();
		final String lon = new Double(location.getLongitude()).toString();
		uri.appendQueryParameter("lat", lat);
		uri.appendQueryParameter("lng", lon);

		final HttpRequest httpRequest = new HttpGet(uri.toString());
		// httpRequest.addHeader("Accept-Encoding", "gzip");
		Log.d(TAG, "Getting place name, url:" + uri.toString());

		final HttpClient httpClient = new DefaultHttpClient();
		final HttpHost httpHost = new HttpHost("ws.geonames.org");
		HttpResponse httpResponse = null;
		httpResponse = httpClient.execute(httpHost, httpRequest);

		if (httpResponse.getStatusLine().getStatusCode() != 200) {
			final int responseCode = httpResponse.getStatusLine()
					.getStatusCode();
			throw new HttpResponseException(responseCode,
					"Trouble with response from geonames: Response code: "
							+ responseCode);
		}

		final InputStream inputStream = httpResponse.getEntity().getContent();
		final SAXParserFactory spf = SAXParserFactory.newInstance();
		final SAXParser parser = spf.newSAXParser();

		final FindNearestPlaceParser findNearestPlaceParser = new FindNearestPlaceParser();

		parser.parse(inputStream, findNearestPlaceParser);

		final String place = findNearestPlaceParser.getName();
		String returnString = "";
		final double distance = findNearestPlaceParser.getDistance();
		if (distance > .5) {
			final Formatter f = new Formatter();
			returnString = f.format("%1.1f", distance).toString()
					+ mContext.getResources().getString(R.string.km_from_);
		}

		returnString += place;

		return returnString;
	}

	private class FindNearestPlaceParser extends DefaultHandler {
		String place = "";
		private boolean isInName = false;
		private boolean isInDistance = false;
		private double distance = 0;

		@Override
		public void characters(final char[] ch, final int start,
				final int length) throws SAXException {
			super.characters(ch, start, length);

			final String value = new String(ch, start, length);
			if (isInName)
				place = value;
			else if (isInDistance)
				distance = new Double(value);
		}

		@Override
		public void endElement(final String uri, final String localName,
				final String qName) throws SAXException {
			// TODO Auto-generated method stub
			super.endElement(uri, localName, qName);
			if (qName.equals("name"))
				isInName = false;
			else if (qName.equals("distance"))
				isInDistance = false;

		}

		public Double getDistance() {
			return distance;
		}

		public String getName() {
			return place;
		}

		@Override
		public void startElement(final String uri, final String localName,
				final String qName, final Attributes attributes)
				throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			if (qName.equals("name"))
				isInName = true;
			else if (qName.equals("distance"))
				isInDistance = true;
		}
	}
}
