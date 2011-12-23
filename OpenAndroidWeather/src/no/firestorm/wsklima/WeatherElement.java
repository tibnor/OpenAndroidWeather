package no.firestorm.wsklima;

import java.util.Date;

/**
 * Weather element that stores one data point for the weather.
 * 
 * Date is the time when the weather was measured. Type is the weather type.
 * Value is the measured value as specified in WeatherType
 */
public class WeatherElement {

	/** Date and time when the weather was measured. */
	private Date date;

	/** Weather type that was measured. */
	private WeatherType type;

	/** Value of the measurement, unit as specified in WeatherType. */
	private String value;

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
	public WeatherElement(Date date, WeatherType type, String value) {
		super();
		this.date = date;
		this.type = type;
		this.value = value;
	}

	/**
	 * Gets the date.
	 * 
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Gets the date.
	 * 
	 * @return the date in milliseconds from Unix epoch time @see
	 *         java.util.Date#getTime()
	 */
	public long getTime() {
		return date.getTime();
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
	public void setDate(Date date) {
		this.date = date;
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
