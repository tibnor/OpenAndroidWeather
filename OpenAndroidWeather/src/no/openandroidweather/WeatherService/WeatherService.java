package no.openandroidweather.WeatherService;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

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

	@Override
	public IBinder onBind(Intent intent) {
		if (IWeatherService.class.getName().equals(intent.getAction()))
			return mBind;

		return null;
	}

	private final IWeatherService.Stub mBind = new IWeatherService.Stub() {
		
		@Override
		public void removeForecastEventListener(IForecastEventListener listener)
				throws RemoteException {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Not implemented!");
		}
		
		@Override
		public void forceUpdateForecasts(int[] rowIds) throws RemoteException {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Not implemented!");
		}
		
		@Override
		public void addForecastEventListener(IForecastEventListener listener)
				throws RemoteException {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Not implemented!");
		}
	};


}
