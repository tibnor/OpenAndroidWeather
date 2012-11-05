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
import android.text.format.DateFormat;
import android.text.format.Time;

import java.util.LinkedList;
import java.util.List;
import no.firestorm.R;
import no.firestorm.ui.stationpicker.Station;
import no.firestorm.weathernotificatonservice.WeatherNotificationService;
import no.firestorm.weathernotificatonservice.WeatherNotificationSettings;
import no.firestorm.wsklima.GetWeather;
import no.firestorm.wsklima.WeatherElement;
import no.firestorm.wsklima.WeatherType;
import no.firestorm.wsklima.WsKlimaProxy;
import no.firestorm.wsklima.exception.NoLocationException;

import org.apache.http.HttpException;
import org.apache.http.client.ClientProtocolException;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

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
				updateReport(false);
			}
		});
		Button retryButton = (Button) findViewById(R.id.retry);
		retryButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateReport(true);
			}
		});
		updateReport(true);

	}

	private void updateReport(boolean firstTime) {
		// Find station
		if (firstTime)
			findViewById(R.id.progressBar).setVisibility(View.GONE);
		else
			findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
		findViewById(R.id.get_weather).setVisibility(View.GONE);
		setVisibility(View.GONE, View.INVISIBLE, View.VISIBLE, View.GONE,
				View.INVISIBLE);
		new FindStationTask().execute();
	}

	private void displayWeather(List<WeatherElement> weather) {
		if (weather == null)
			return;

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
			case precipitation:
				if (w.getValue() != "")
					setText(R.id.precipitation, w.getValue() + " mm");
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

		setVisibility(View.GONE, View.VISIBLE, View.GONE, View.VISIBLE,
				View.VISIBLE);
		findViewById(R.id.progressBar).setVisibility(View.GONE);
		findViewById(R.id.get_weather).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.info)).setText(message);
	}

	private void setVisibility(int report, int info, int progress, int retry,
			int empty) {
		findViewById(R.id.report).setVisibility(report);
		findViewById(R.id.info).setVisibility(info);
		findViewById(R.id.retry).setVisibility(retry);
		findViewById(R.id.progressBarMain).setVisibility(progress);
		findViewById(R.id.empty).setVisibility(empty);
	}

	private class FindStationTask extends AsyncTask<Void, Void, WeatherElement> {
		private Throwable error = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

		}

		@Override
		protected WeatherElement doInBackground(Void... params) {
			final Context context = WeatherReportActivity.this;
			boolean isUsingClosestStation = WeatherNotificationSettings
					.isUsingNearestStation(context);
			if (isUsingClosestStation) {
				GetWeather gw = new GetWeather(WeatherReportActivity.this);
				try {
					WeatherElement w = gw.getWeatherElement();
					if (w != null)
						return w;
					else
						error = new NetworkErrorException();
				} catch (NetworkErrorException e) {
					error = e;
				} catch (NoLocationException e) {
					error = e;
				} catch (HttpException e) {
					error = e;
				}
			} else {
				int stationId = WeatherNotificationSettings
						.getStationId(context);
				String stationName = WeatherNotificationSettings
						.getStationName(context);
				Station station = new Station(stationName, stationId, 0, 0,
						null, true);
				WsKlimaProxy proxy = new WsKlimaProxy();
				WeatherElement weather;
				try {
					weather = proxy.getTemperatureNow(stationId, context);
					if (weather != null) {
						weather.setStation(station);
						return weather;
					} else
						error = new NetworkErrorException("No data");
				} catch (NetworkErrorException e) {
					error = e;
				} catch (HttpException e) {
					error = e;
				}

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
			setVisibility(View.VISIBLE, View.GONE, View.GONE, View.GONE,
					View.GONE);

			final java.text.DateFormat df = DateFormat
					.getTimeFormat(WeatherReportActivity.this);
			setText(R.id.time, df.format(w.getTime()));
			new GetWeatherTask1().execute();
			new GetWeatherTask2().execute();
			new GetPersipitationTask().execute();

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
			intent.putExtra(WeatherNotificationService.INTENT_EXTRA_INFO_TIME,
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
				return proxy.getWeather(mStationId, 2, "FF,DD,TA,FG_1");
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
			if (result != null && result.size() > 0)
				displayWeather(result);
		}

	}

	private class GetWeatherTask2 extends
			AsyncTask<Integer, Void, List<WeatherElement>> {

		@Override
		protected List<WeatherElement> doInBackground(Integer... params) {
			WsKlimaProxy proxy = new WsKlimaProxy();
			try {
				return proxy.getWeather(mStationId, 0, "TAX,TAN,FGX,FXX");
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
			if (result != null && result.size() > 0)
				displayWeather(result);
			findViewById(R.id.progressBar).setVisibility(View.GONE);
			findViewById(R.id.get_weather).setVisibility(View.VISIBLE);
		}

	}

	private class GetPersipitationTask extends
			AsyncTask<Integer, Void, List<WeatherElement>> {

		@Override
		protected List<WeatherElement> doInBackground(Integer... params) {
			WsKlimaProxy proxy = new WsKlimaProxy();
			try {
				Time now = new Time();
				now.setToNow();
				now.switchTimezone("UTC");
				String hours = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24";
				List<WeatherElement> res = proxy.getWeather(mStationId, 2,
						"RR_1", now, hours);
				Double value = 0.;
				if (res != null && !res.isEmpty()) {
					for (WeatherElement w : res) {
						value += Double.parseDouble(w.getValue());
					}
					WeatherElement w = res.get(res.size() - 1);
					w.setValue(String.format("%.1f", value));
					w.setType(WeatherType.precipitation);
					res = new LinkedList<WeatherElement>();
					res.add(w);
					return res;
				} else {
					return null;
				}
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
			if (result != null) {
				displayWeather(result);
				findViewById(R.id.progressBar).setVisibility(View.GONE);
				findViewById(R.id.get_weather).setVisibility(View.VISIBLE);
			}
		}

	}
}
