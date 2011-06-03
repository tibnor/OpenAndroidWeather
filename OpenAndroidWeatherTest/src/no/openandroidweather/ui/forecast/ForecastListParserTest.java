package no.openandroidweather.ui.forecast;

import java.util.List;

import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

public class ForecastListParserTest extends AndroidTestCase{
	public void testParse(){
		/*
		ContentResolver cr = getContext().getContentResolver();
		String[] projection = {WeatherContentProvider.Meta._ID};
		Cursor c = cr.query(WeatherContentProvider.CONTENT_URI, projection , null, null, null);
		c.moveToFirst();
		int id = c.getInt(c.getColumnIndex(WeatherContentProvider.Meta._ID));
		Uri uri = Uri.withAppendedPath(WeatherContentProvider.CONTENT_URI, id+"");
		assertNotNull(uri);
		
		ForecastListParser parser = new ForecastListParser(getContext(), null);
		List<IListRow> rows = parser.parseData(uri);
		assertNotNull(rows);
		assertTrue(rows.size()>0);
		*/
		
	}
}
