package no.firestorm.wsklima;

import java.util.Date;

public class WeatherElement {
	private Date fromDate;
	private Date toDate;
	private WeatherType type;
	private String value;

	public WeatherElement() {
	}

	public WeatherElement(Date fromDate, Date toDate, WeatherType type,
			String value) {
		super();
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.type = type;
		this.value = value;
	}

	public Date getFrom() {
		return fromDate;
	}

	public Date getTo() {
		return toDate;
	}

	public WeatherType getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public void setFrom(Date from) {
		fromDate = from;
	}

	public void setTo(Date to) {
		toDate = to;
	}

	public void setType(String type) {
		if (type.equals("TA"))
			this.type = WeatherType.temperature;
		else
			throw new UnsupportedOperationException("Unknown type");
	}

	public void setType(WeatherType type) {
		this.type = type;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
