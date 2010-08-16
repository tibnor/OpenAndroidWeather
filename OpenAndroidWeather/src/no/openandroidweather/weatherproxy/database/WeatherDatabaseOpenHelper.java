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

package no.openandroidweather.weatherproxy.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class WeatherDatabaseOpenHelper extends SQLiteOpenHelper {
	private static final String TAG = "WeatherProxyDatabaseOpenHelper"; 
	private static final String DATABASE_FILE = "weather.db";
	static final int DATABASE_VERSION = 1;
	private static final String CREATE_DB = WeatherProxyDatabase.FORECAST_CREATE_TABLE
			+ "; " + WeatherProxyDatabase.META_CREATE_TABLE;

	public WeatherDatabaseOpenHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	public WeatherDatabaseOpenHelper(Context context) {
		super(context, DATABASE_FILE, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.v(TAG, WeatherProxyDatabase.META_CREATE_TABLE + WeatherProxyDatabase.FORECAST_CREATE_TABLE);
		db.execSQL(WeatherProxyDatabase.META_CREATE_TABLE);
		db.execSQL(WeatherProxyDatabase.FORECAST_CREATE_TABLE);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion==DATABASE_VERSION)
			return;
			
		deleteDatabase(db);
		onCreate(db);
	}

	void deleteDatabase(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS "+WeatherProxyDatabase.FORECAST_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS "+WeatherProxyDatabase.META_TABLE_NAME);
	}

}
