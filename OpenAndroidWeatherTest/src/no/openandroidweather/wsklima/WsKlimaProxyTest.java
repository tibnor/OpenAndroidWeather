package no.openandroidweather.wsklima;

import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

public class WsKlimaProxyTest extends TestCase {
	WsKlimaProxy proxy = new WsKlimaProxy();

	public void testGetWeatherNow() {
		List<WeatherElement> result = proxy.getWeatherNow(68860, 3600 * 10);
		assertNotNull(result);
		assertNotNull(result.get(0).getValue());
	}
	
	public void testGetWeather() {
		List<WeatherElement> result = proxy.getWeather("2", "2011-02-14", "2011-02-14", "68860", "TA", "00", "2", "");
		assertNotNull(result);
		WeatherElement answer = result.get(0);
		Date from = answer.getFrom();
		assertEquals(new Date(1297670400000l), from);
		assertNull(answer.getTo());
		assertEquals(2,answer.getType());
		assertEquals("-5", answer.getValue());
	}

}
