package no.openandroidweather.weatherproxy;

import android.location.Location;

public interface WeatherProxy {

	/**
	 * Gets the newest forecast, if successful it returns the rowId where the
	 * forecast is added in the meta table. TODO:It also throws errors
	 * 
	 * @param location
	 *            of the forecast
	 * @return rowId in the content provider.
	 */
	int getWeatherForecast(Location location);
}
