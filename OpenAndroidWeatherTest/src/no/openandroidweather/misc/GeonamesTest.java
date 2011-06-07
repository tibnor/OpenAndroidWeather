package no.openandroidweather.misc;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.location.Location;
import android.test.AndroidTestCase;

public class GeonamesTest extends AndroidTestCase {
	public void testFindNearestPlace() throws ClientProtocolException, IOException, ParserConfigurationException, SAXException{
		Location loc = new Location("");
		loc.setLatitude(63.42);
		loc.setLongitude(10.432);
		
		Geonames geo = new Geonames(getContext());
		String result = geo.findNearestPlace(loc);
		assertEquals("1,7km from Persaunet",result);
	}
	
}
