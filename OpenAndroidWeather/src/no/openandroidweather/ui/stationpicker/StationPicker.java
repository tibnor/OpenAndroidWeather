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

package no.openandroidweather.ui.stationpicker;

import java.util.Date;
import java.util.TimeZone;

import no.openandroidweather.R;
import no.openandroidweather.misc.TempToDrawable;
import no.openandroidweather.ui.notificationpreferences.NotificationPreferences;
import no.openandroidweather.wsklima.WeatherElement;
import no.openandroidweather.wsklima.WsKlimaProxy;
import no.openandroidweather.wsklima.database.WsKlimaDataBaseHelper;

import org.apache.http.HttpException;

import android.accounts.NetworkErrorException;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class StationPicker extends ListActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stationslist);

		StationQuery stationQuery = new StationQuery(this);
		Cursor c = stationQuery.runQuery("");
		startManagingCursor(c);

		String[] from = { WsKlimaDataBaseHelper.STATIONS_NAME };
		int[] to = { R.id.StationName };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.stationslist_row, c, from, to);
		adapter.setFilterQueryProvider(stationQuery);

		setListAdapter(adapter);
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		// get temp
		final WsKlimaProxy weatherProxy = new WsKlimaProxy();
		WeatherElement temperature;
		try {

			temperature = weatherProxy.getTemperatureNow((int) id, 3600 * 24);
			final int icon = TempToDrawable.getDrawableFromTemp(Float
					.valueOf(temperature.getValue()));
			final CharSequence tickerText = "Temperatur";
			final long when = System.currentTimeMillis();

			final Notification notification = new Notification(icon,
					tickerText, when);

			final Context context = getApplicationContext();
			final CharSequence contentTitle = "Temperatur";
			final Date time = temperature.getFrom();

			java.text.DateFormat fmt = DateFormat.getTimeFormat(this);
			
			final CharSequence contentText = "Temperatur: "
					+ temperature.getValue() + " Tid:" + fmt.format(time);
			final Intent notificationIntent = new Intent(this,
					NotificationPreferences.class);
			final PendingIntent contentIntent = PendingIntent.getActivity(this,
					0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText,
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

	class StationQuery implements FilterQueryProvider {
		SQLiteDatabase mDb;

		public StationQuery(Context context) {
			WsKlimaDataBaseHelper dbHelper = new WsKlimaDataBaseHelper(context);
			mDb = dbHelper.getReadableDatabase();
		}

		@Override
		public Cursor runQuery(CharSequence constraint) {
			String[] select = { WsKlimaDataBaseHelper.STATIONS_ID,
					WsKlimaDataBaseHelper.STATIONS_NAME };
			String orderBy = WsKlimaDataBaseHelper.STATIONS_NAME;
			String selection = WsKlimaDataBaseHelper.STATIONS_NAME + " LIKE '%"
					+ constraint.toString().toUpperCase() + "%'";
			return mDb.query(WsKlimaDataBaseHelper.STATIONS_TABLE_NAME, select,
					selection, null, null, null, orderBy);
		}

	}

}
