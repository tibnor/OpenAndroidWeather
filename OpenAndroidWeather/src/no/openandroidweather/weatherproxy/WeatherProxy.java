package no.openandroidweather.weatherproxy;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public interface WeatherProxy {
	
	// Gets the latest weather forecast
	Cursor query(Uri uri, ContentValues values, String selection, String[] selectionArgs);
}
