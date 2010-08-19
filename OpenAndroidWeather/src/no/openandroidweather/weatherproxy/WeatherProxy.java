package no.openandroidweather.weatherproxy;

import java.io.IOException;

import android.location.Location;
import android.net.Uri;

public interface WeatherProxy {

	/**
	 * Gets the newest forecast, if successful it returns the rowId where the
	 * forecast is added in the meta table. TODO:It also throws errors
	 * 
	 * @param location
	 *            of the forecast
	 * @return Uri in the content provider.
	 */
	Uri getWeatherForecast(Location location) throws IOException, Exception ;
}
