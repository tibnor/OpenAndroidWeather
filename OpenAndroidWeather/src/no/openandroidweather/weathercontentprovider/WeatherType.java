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

package no.openandroidweather.weathercontentprovider;

public class WeatherType {
	public static final int symbol = 0;
	public static final int precipitation = 1;
	public static final int temperature = 2;
	public static final int windSpeed = 3;
	public static final int windDirection = 4;

	public class symbol {
		public static final int SUN = 1;
		public static final int LIGHTCLOUD = 2;
		public static final int PARTLYCLOUD = 3;
		public static final int CLOUD = 4;
		public static final int LIGHTRAINSUN = 5;
		public static final int LIGHTRAINTHUNDERSUN = 6;
		public static final int SLEETSUN = 7;
		public static final int SNOWSUN = 8;
		public static final int LIGHTRAIN = 9;
		public static final int RAIN = 10;
		public static final int RAINTHUNDER = 11;
		public static final int SLEET = 12;
		public static final int SNOW = 13;
		public static final int SNOWTHUNDER = 14;
		public static final int FOG = 15;
	}
}
