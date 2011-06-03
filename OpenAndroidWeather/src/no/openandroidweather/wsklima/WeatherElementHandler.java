package no.openandroidweather.wsklima;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class WeatherElementHandler extends DefaultHandler {
	final private static SimpleDateFormat timeFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
	private final ArrayList<WeatherElement> allElements = new ArrayList<WeatherElement>();
	private WeatherElement element = new WeatherElement();
	private Date fromTime = null;
	private Boolean isInLocationItem = false;
	private Boolean isInTimeItem = false;
	private Boolean isInWeatherElement = false;
	private String lastLocalName = null;
	private Date toTime = null;
	private String weatherId = null;
	private String weatherValue = null;
	private String buffer = "";
	
	static{
		timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Gets be called on the following structure: <tag>characters</tag>
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		String value = new String(ch, start, length);
		if (!value.equals("\n"))
			buffer += new String(ch, start, length);
	}

	/**
	 * Gets be called on closing tags like: </tag>
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		// Save data
		if (isInWeatherElement) {
			if (lastLocalName.equals("value"))
				// Save value
				weatherValue = buffer;
			else if (lastLocalName.equals("id"))
				// Save weather type
				weatherId = buffer;

		} else if (isInTimeItem) {
			// Save time
			if (lastLocalName.equals("to"))
				toTime = parseDate(buffer);
			if (lastLocalName.equals("from"))
				fromTime = parseDate(buffer);
		}
		
		buffer = "";
		
		if (qName.equals("item"))
			if (isInWeatherElement) {
				isInWeatherElement = false;
				newWeatherElementPoint();
			} else if (isInLocationItem)
				isInLocationItem = false;
			else if (isInTimeItem) {
				isInTimeItem = false;
				toTime = null;
				fromTime = null;
			}

	}

	private Date parseDate(String date) {
		if (date == null || date.equals(""))
			return null;
		else {
			try {
				return timeFormat.parse(date);
			} catch (final ParseException e) {
				e.printStackTrace();
				return null;
			}
		}
		
	}

	public ArrayList<WeatherElement> getAllElements() {
		return allElements;
	}

	private void newWeatherElementPoint() {
		element.setType(weatherId);
		element.setValue(weatherValue);
		element.setFrom(fromTime);
		element.setTo(toTime);
		allElements.add(element);
		element = new WeatherElement();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) {
		if (qName.equals("item")) {
			final String type = attributes.getValue(0);
			if (type.equals("ns2:no_met_metdata_TimeStamp"))
				isInTimeItem = true;
			else if (type.equals("ns2:no_met_metdata_Location"))
				isInLocationItem = true;
			else if (type.equals("ns2:no_met_metdata_WeatherElement"))
				isInWeatherElement = true;
		} else
			lastLocalName = qName;
	}
}
