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

package no.openandroidweather.ui.notificationpreferences;

import java.util.Date;

import no.openandroidweather.R;
import no.openandroidweather.misc.TempToDrawable;
import no.openandroidweather.wsklima.WeatherElement;
import no.openandroidweather.wsklima.WsKlimaProxy;
import no.openandroidweather.wsklima.database.WsKlimaDataBaseHelper;

import org.apache.http.HttpException;

import android.accounts.NetworkErrorException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class NotificationPreferences extends PreferenceActivity {
	private static final int HELLO_ID = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.notifier_preferences);

		final String ns = Context.NOTIFICATION_SERVICE;
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		WsKlimaDataBaseHelper dbHelper = new WsKlimaDataBaseHelper(this);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		
		
		// get temp
		final WsKlimaProxy weatherProxy = new WsKlimaProxy();
		WeatherElement temperature;
		try {
			temperature = weatherProxy.getTemperatureNow(28380, 3600 * 24);
			final int icon = TempToDrawable.getDrawableFromTemp(Float
					.valueOf(temperature.getValue()));
			final CharSequence tickerText = "Kongsberg";
			final long when = System.currentTimeMillis();

			final Notification notification = new Notification(icon,
					tickerText, when);

			final Context context = getApplicationContext();
			final CharSequence contentTitle = "Kongsberg";
			final Date time = temperature.getFrom();

			final CharSequence contentText = "Temperatur: "
					+ temperature.getValue() + " Tid:" + time.toString();
			final Intent notificationIntent = new Intent(this,
					NotificationPreferences.class);
			final PendingIntent contentIntent = PendingIntent.getActivity(this,
					0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText,
					contentIntent);

			mNotificationManager.notify(HELLO_ID, notification);
		} catch (final NetworkErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
