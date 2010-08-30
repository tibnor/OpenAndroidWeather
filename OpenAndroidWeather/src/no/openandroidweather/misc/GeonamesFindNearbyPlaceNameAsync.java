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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;

public class GeonamesFindNearbyPlaceNameAsync extends
		AsyncTask<Location, Void, String> {
	private Context mContext;
	private boolean isDone = false;
	private String place;
	
	public GeonamesFindNearbyPlaceNameAsync(Context context){
		super();
		mContext = context;
	}

	public boolean isDone() {
		return isDone;
	}

	@Override
	protected String doInBackground(Location... arg0) {
		Geonames geonames = new Geonames(mContext);
		try {
			place = geonames.findNearestPlace(arg0[0]);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		synchronized (this) {
			isDone = true;
			this.notifyAll();
		}
		return place;
	}
	
	public String getPlace(){
		return place;
	}

}
