/*
	Copyright 2012 Torstein Ingebrigtsen Bø

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

package no.firestorm.ui.weatherreport;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpException;
import org.apache.http.client.ClientProtocolException;

import no.firestorm.R;
import no.firestorm.ui.stationpicker.Station;
import no.firestorm.wsklima.GetWeather;
import no.firestorm.wsklima.WeatherElement;
import no.firestorm.wsklima.WsKlimaProxy;
import no.firestorm.wsklima.exception.NoLocationException;
import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class WeatherReportActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.weatherreport);
		// Find station
		GetWeather gw = new GetWeather(this);
		try {
			WeatherElement w = gw.getWeatherElement();
			setText(R.id.temperature, w.getValue()+" °C");
			Station station = gw.getStation();
			setText(R.id.StationName, station.getName());
			WsKlimaProxy proxy = new WsKlimaProxy();
			List<WeatherElement> weather = proxy.getWeather(station.getId(), "TAX,TAN,FF,DD,TA,RR_1,RR_12");
			displayWeather(weather);
		} catch (NetworkErrorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (HttpException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private void displayWeather(List<WeatherElement> weather){
		TextView timeView = (TextView) findViewById(R.id.time);
		final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT,Locale.getDefault());
		timeView.setText(df.format(weather.get(0).getTime()));
		for (WeatherElement w : weather) {
			switch (w.getType()) {
			case temperature:
				setText(R.id.temperature,w.getValue()+" °C");
				break;
			case temperatureMax:
				setText(R.id.temperatureMax,w.getValue()+" °C");
				break;
			case temperatureMin:
				setText(R.id.temperatureMin,w.getValue()+" °C");
				break;
			case windDirection:
				setText(R.id.windDirection,w.getValue()+"°");
				break;
			case windSpeed:
				setText(R.id.windSpeed,w.getValue()+" m/s");
				break;
			case precipitationLast12h:
				setText(R.id.precipitationLast12h,w.getValue()+" mm");
				break;
			case precipitationLastHour:
				setText(R.id.precipitationLast12h,w.getValue()+" mm");
				break;
			default:
				break;
			}
		}
	}
	
	private void setText(int id, String value){
		TextView v = (TextView) findViewById(id);
		v.setText(value);
	}
}
