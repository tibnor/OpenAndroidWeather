/*
	Copyright 2011 Torstein Ingebrigtsen Bø

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

package no.openandroidweather.ui;

import java.util.Date;

import org.apache.http.HttpException;

import no.openandroidweather.R;
import no.openandroidweather.misc.TempToDrawable;
import no.openandroidweather.ui.notificationpreferences.NotificationPreferences;
import no.openandroidweather.ui.stationpicker.StationPicker;
import no.openandroidweather.wsklima.WeatherElement;
import no.openandroidweather.wsklima.WsKlimaProxy;
import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Settings extends Activity {
	private static final int ACTIVITY_CHOOSE_STATION = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		// Add station name
		setStationName();

		// Set onClickEvent for choose station
		setChooseStationButtion();

		// TODO: Set onClickEvent for get weather
		setGetWeatherButton();
	}

	private void setGetWeatherButton() {
		Button chooseStationButton = (Button) findViewById(R.id.get_weather);
		chooseStationButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				showTemp();
			}
		});
	}

	private void setChooseStationButtion() {
		Button chooseStationButton = (Button) findViewById(R.id.choose_station);
		chooseStationButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				final Intent intent = new Intent(Settings.this,
						StationPicker.class);
				startActivityForResult(intent, ACTIVITY_CHOOSE_STATION);
			}
		});
	}

	private void setStationName() {
		TextView stationNameView = (TextView) findViewById(R.id.StationName);
		SharedPreferences settings = getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		String stationName = settings.getString(
				WsKlimaProxy.PREFS_STATION_NAME_KEY,
				WsKlimaProxy.PREFS_STATION_NAME_DEFAULT);
		stationNameView.setText(stationName);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// Handle chosen station
		switch (requestCode) {
		case ACTIVITY_CHOOSE_STATION:
			// Update station name if changed
			if (resultCode == RESULT_OK)
				setStationName();
			break;
		default:
			break;
		}
	}

	private void showTemp() {
		// Find station id
		SharedPreferences settings = getSharedPreferences(
				WsKlimaProxy.PREFS_NAME, 0);
		String stationName = settings.getString(
				WsKlimaProxy.PREFS_STATION_NAME_KEY,
				WsKlimaProxy.PREFS_STATION_NAME_DEFAULT);
		int stationId = settings.getInt(WsKlimaProxy.PREFS_STATION_ID_KEY,
				WsKlimaProxy.PREFS_STATION_ID_DEFAULT);

		// get temp
		final WsKlimaProxy weatherProxy = new WsKlimaProxy();
		WeatherElement temperature;
		try {

			temperature = weatherProxy.getTemperatureNow(stationId, 3600 * 24);
			final int icon = TempToDrawable.getDrawableFromTemp(Float
					.valueOf(temperature.getValue()));
			final CharSequence tickerText = stationName;
			final long when = System.currentTimeMillis();

			final Notification notification = new Notification(icon,
					tickerText, when);

			final CharSequence contentTitle = stationName;
			final Date time = temperature.getFrom();

			java.text.DateFormat fmt = DateFormat.getTimeFormat(this);

			final CharSequence contentText = "Temperatur: "
					+ temperature.getValue() + " Tid: " + fmt.format(time);
			final Intent notificationIntent = new Intent(this,
					NotificationPreferences.class);
			final PendingIntent contentIntent = PendingIntent.getActivity(this,
					0, notificationIntent, 0);

			notification.setLatestEventInfo(this, contentTitle, contentText,
					contentIntent);

			final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(1, notification);
		} catch (final NetworkErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
