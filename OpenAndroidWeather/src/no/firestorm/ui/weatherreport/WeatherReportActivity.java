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
    GNU General Public License for more details.			new GetWeatherTask().execute();

    You should have received a copy of the GNU General Public License
    along with OpenAndroidWeather.  If not, see <http://www.gnu.org/licenses/>.
 */

package no.firestorm.ui.weatherreport;

import java.io.IOException;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;

import no.firestorm.R;
import no.firestorm.ui.stationpicker.Station;
import no.firestorm.weathernotificatonservice.WeatherNotificationService;
import no.firestorm.wsklima.GetWeather;
import no.firestorm.wsklima.WeatherElement;
import no.firestorm.wsklima.WsKlimaProxy;
import no.firestorm.wsklima.exception.NoLocationException;

import org.apache.http.HttpException;
import org.apache.http.client.ClientProtocolException;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class WeatherReportActivity extends Activity {
	private Integer mStationId = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.weatherreport);
		ImageButton updateButton = (ImageButton) findViewById(R.id.get_weather);
		updateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateReport();
			}
		});

		updateReport();

	}

	private void updateReport() {
		// Find station
		new FindStationTask().execute();
	}

	private void displayWeather(List<WeatherElement> weather) {

		for (WeatherElement w : weather) {
			switch (w.getType()) {
			case temperature:
				setText(R.id.temperature, w.getValue() + " °C");
				break;
			case temperatureMax:
				setText(R.id.temperatureMax, w.getValue() + " °C");
				break;
			case temperatureMin:
				setText(R.id.temperatureMin, w.getValue() + " °C");
				break;
			case windDirection:
				setText(R.id.windDirection, w.getValue() + "°");
				break;
			case windSpeed:
				setText(R.id.windSpeed, w.getValue());
				break;
			case windGustSpeedMax:
				setText(R.id.windGustSpeedMax, "(" + w.getValue() + ")");
				break;
			case windSpeedMax:
				setText(R.id.windSpeedMax, w.getValue());
				break;
			case windGustSpeed:
				setText(R.id.windGustSpeed, "(" + w.getValue() + ")");
				break;
			case precipitationLast12h:
				setText(R.id.precipitationLast12h, w.getValue() + " mm");
				break;
			case precipitationLastHour:
				setText(R.id.precipitationLast12h, w.getValue() + " mm");
				break;
			default:
				break;
			}
		}
	}

	private void setText(int id, String value) {
		TextView v = (TextView) findViewById(id);
		v.setText(value);
	}

	private void displayError(Throwable e) {
		int message = 0;
		if (e instanceof NetworkErrorException) {
			message = R.string.error_download_long;
		} else if (e instanceof HttpException) {
			message = R.string.error_server_long;
		} else if (e instanceof NoLocationException) {
			message = R.string.error_location_long;
		} else {
			e.printStackTrace();
			throw new UnknownError();
		}

		Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.show();
		findViewById(R.id.progressBar).setVisibility(View.GONE);
		findViewById(R.id.get_weather).setVisibility(View.VISIBLE);
	}

	private class FindStationTask extends AsyncTask<Void, Void, WeatherElement> {
		private Throwable error = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
			findViewById(R.id.get_weather).setVisibility(View.GONE);
		}

		@Override
		protected WeatherElement doInBackground(Void... params) {
			GetWeather gw = new GetWeather(WeatherReportActivity.this);
			try {
				return gw.getWeatherElement();
			} catch (NetworkErrorException e) {
				error = e;
			} catch (NoLocationException e) {
				error = e;
			} catch (HttpException e) {
				error = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(WeatherElement w) {
			if (error != null) {
				displayError(error);
				return;

			}
			setText(R.id.temperature, w.getValue() + " °C");
			Station station = w.getStation();
			setText(R.id.StationName, station.getName());
			mStationId = station.getId();

			TextView timeView = (TextView) findViewById(R.id.time);
			final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT,
					Locale.getDefault());
			timeView.setText(df.format(w.getTime()));
			new GetWeatherTask1().execute();

			// Update notification:
			Intent intent = new Intent(WeatherReportActivity.this,
					WeatherNotificationService.class);
			intent.putExtra(
					WeatherNotificationService.INTENT_EXTRA_ACTION,
					WeatherNotificationService.INTENT_EXTRA_ACTION_SET_NOTIFICATION);
			intent.putExtra(
					WeatherNotificationService.INTENT_EXTRA_INFO_STATION_NAME,
					station.getName());
			intent.putExtra(
					WeatherNotificationService.INTENT_EXTRA_INFO_TEMPERATURE,
					w.getValue());
			intent.putExtra(
					WeatherNotificationService.INTENT_EXTRA_INFO_TIME,
					w.getTime());
			startService(intent);
		}

	}

	private class GetWeatherTask1 extends
			AsyncTask<Integer, Void, List<WeatherElement>> {

		@Override
		protected List<WeatherElement> doInBackground(Integer... params) {
			WsKlimaProxy proxy = new WsKlimaProxy();
			try {
				return proxy.getWeather(mStationId, 2,
						"FF,DD,TA,RR_1,RR_12,FG_1");
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(List<WeatherElement> result) {
			displayWeather(result);
			new GetWeatherTask2().execute();
		}

	}

	private class GetWeatherTask2 extends
			AsyncTask<Integer, Void, List<WeatherElement>> {

		@Override
		protected List<WeatherElement> doInBackground(Integer... params) {
			WsKlimaProxy proxy = new WsKlimaProxy();
			try {
				return proxy.getWeather(mStationId, 0, "TAX,TAN,FGX,FFX");
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(List<WeatherElement> result) {
			displayWeather(result);
			findViewById(R.id.progressBar).setVisibility(View.GONE);
			findViewById(R.id.get_weather).setVisibility(View.VISIBLE);
		}

	}
}
