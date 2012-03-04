package no.firestorm.wsklima;

import no.firestorm.ui.stationpicker.Station;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

/**
 * Weather element that stores one data point for the weather.
 * 
 * Date is the time when the weather was measured. Type is the weather type.
 * Value is the measured value as specified in WeatherType
 */
public class WeatherElement{

	/** Date and time when the weather was measured. */
	private Time date;

	/** Weather type that was measured. */
	private WeatherType type;

	/** Value of the measurement, unit as specified in WeatherType. */
	private String value;

	/** Weather station for the observation */
	private Station station;

	/**
	 * Instantiates a new weather element.
	 * 
	 * @param date
	 *            the time
	 * @param type
	 *            the type
	 * @param value
	 *            the value
	 */
	public WeatherElement(Time date, WeatherType type, String value) {
		this(date,type,value,null);
	}

	public WeatherElement(Time date, WeatherType type, String value,
			Station station) {
		super();
		this.date = date;
		this.type = type;
		this.value = value;
		this.station = station;
	}	

	/**
	 * Gets the date.
	 * 
	 * @return the date
	 */
	public Time getDate() {
		return date;
	}

	/**
	 * Gets the date.
	 * 
	 * @return the date in milliseconds from Unix epoch time @see
	 *         java.util.Date#getTime()
	 */
	public long getTime() {
		return date.toMillis(false);
	}

	/**
	 * Gets the type.
	 * 
	 * @return the type
	 */
	public WeatherType getType() {
		return type;
	}

	/**
	 * Gets the value.
	 * 
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the date.
	 * 
	 * @param date
	 *            the new date
	 */
	public void setDate(Time date) {
		this.date = date;
	}

	public Station getStation() {
		return station;
	}

	public void setStation(Station station) {
		this.station = station;
	}

	/**
	 * Sets the type.
	 * 
	 * @param type
	 *            the new type
	 */
	public void setType(String type) {
		if (type.equals("TA"))
			this.type = WeatherType.temperature;
		else
			throw new UnsupportedOperationException("Unknown type");
	}

	/**
	 * Sets the type.
	 * 
	 * @param type
	 *            the new type
	 */
	public void setType(WeatherType type) {
		this.type = type;
	}

	/**
	 * Sets the value.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setValue(String value) {
		this.value = value;
	}

}
