package no.openandroidweather.wsklima;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

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
	private String buffer = null;

	/**
	 * Gets be called on the following structure: <tag>characters</tag>
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		final String value = new String(ch, start, length);
		
		if (value.equals("\n"))
			;
		else if (isInWeatherElement) {
			if (lastLocalName.equals("value"))
				// Save value
				weatherValue = value;
			else if (lastLocalName.equals("id"))
				// Save weather type
				weatherId = value;

		} else if (isInTimeItem) {
			// Save time
			if (lastLocalName.equals("to"))
				try {
					toTime = timeFormat.parse(value);
				} catch (final ParseException e) {
					e.printStackTrace();
				}
			if (lastLocalName.equals("from"))
				try {
					fromTime = timeFormat.parse(value);
				} catch (final ParseException e) {
					e.printStackTrace();
				}
		}
	}

	@Override
	public void endDocument() throws SAXException {
	}

	/**
	 * Gets be called on closing tags like: </tag>
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
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
