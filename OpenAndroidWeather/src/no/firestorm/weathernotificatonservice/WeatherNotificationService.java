/*
	Copyright 2011 Torstein Ingebrigtsen Bø

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

package no.firestorm.weathernotificatonservice;

import java.text.DateFormat;
import java.util.Date;

import no.firestorm.R;
import no.firestorm.misc.CheckInternetStatus;
import no.firestorm.misc.TempToDrawable;
import no.firestorm.ui.stationpicker.Station;
import no.firestorm.wsklima.GetWeather;
import no.firestorm.wsklima.WeatherElement;
import no.firestorm.wsklima.WsKlimaProxy;
import no.firestorm.wsklima.exception.NoLocationException;

import org.apache.http.HttpException;

import android.accounts.NetworkErrorException;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

/**
 * Updates notification if extra intent is INTENT_EXTRA_ACTION_GET_TEMP, for
 * updating the notification or INTENT_EXTRA_ACTION_UPDATE_ALARM for setting the
 * alarm for the next time the notification should be updated.
 * 
 * Start service for updating notification by:
 * 
 * @code final Intent intent = new Intent(this,
 *       WeatherNotificationService.class);
 *       intent.putExtra(WeatherNotificationService.INTENT_EXTRA_ACTION,
 *       WeatherNotificationService.INTENT_EXTRA_ACTION_GET_TEMP);
 *       startService(intent);
 * 
 * @endcode
 */
public class WeatherNotificationService extends IntentService {

	@SuppressWarnings("unused")
	private static final String LOG_ID = "no.firestorm.weatherservice";

	/**
	 * Key for specifying action when starting the service @see
	 * WeatherNotificationService
	 */
	public static final String INTENT_EXTRA_ACTION = "action";

	/**
	 * If INTENT_EXTRA_ACTION parameter in startup intent is set to this, the
	 * service update the notification.
	 */
	public static final int INTENT_EXTRA_ACTION_GET_TEMP = 1;

	/**
	 * If INTENT_EXTRA_ACTION parameter in startup intent is set to this, the
	 * service update the alarm for updating the notification.
	 */
	public static final int INTENT_EXTRA_ACTION_UPDATE_ALARM = 2;

	/**
	 * 
	 */
	public WeatherNotificationService() {
		super("WeatherNotifivationService");
	}

	/**
	 * @param name
	 */
	public WeatherNotificationService(String name) {
		super(name);
	}

	/**
	 * Add receiver that gets notified if the phone gets connected again, they
	 * will also start the service
	 */
	private void addConnectionChangedReceiver() {
		final Context context = this;
		if (WeatherNotificationSettings.getUpdateRate(context) > 0)
			if (WeatherNotificationSettings.getDownloadOnlyOnWifi(context)) {
				if (!CheckInternetStatus.isWifiConnected(context))
					WifiEnabledIntentReceiver.setEnableReciver(context, true);
			} else if (!CheckInternetStatus.isConnected(context))
				InternetEnabledIntentReceiver.setEnableReciver(context, true);
	}

	/**
	 * Downloads weather data If isUsingNearestStation is false, it doesn't do
	 * update station. If true it update the station.
	 * 
	 * @see UpdateStation
	 * 
	 * @return temperature
	 * @throws NetworkErrorException
	 *             if download problem
	 * @throws HttpException
	 *             if download problem
	 * @throws NoLocationException
	 *             if location provider does not give any location to
	 *             UpdateStation in two minutes
	 */
	protected WeatherElement getWeatherData() throws HttpException,
			NoLocationException, NetworkErrorException {
		final Context context = this;
		boolean isUsingClosestStation = WeatherNotificationSettings
				.isUsingNearestStation(context);

		WeatherElement weather = null;
		// get temp
		try {
			if (isUsingClosestStation) {
				GetWeather getWeather = new GetWeather(this);
				weather  = getWeather.getWeatherElement();
				Station station = getWeather.getStation();
				WeatherNotificationSettings.setStation(context,
						station.getName(), station.getId());
			} else {
				int stationId = WeatherNotificationSettings.getStationId(context);
				WsKlimaProxy proxy = new WsKlimaProxy();
				weather = proxy.getTemperatureNow(stationId, context);
			}

		} catch (final NetworkErrorException e) {
			// Add an receiver thats waits on connection
			addConnectionChangedReceiver();
			throw e;
		}

		
		// Save if data
		if (weather != null)
			WeatherNotificationSettings.setLastTemperature(context,
					weather.getValue(), weather.getDate());
		return weather;
	}

	/**
	 * Shows a notification with information about the error, either NoLocation
	 * or DownloadError (default)
	 * 
	 * @param e
	 *            Exception
	 */
	private void makeNotification(Exception e) {
		int tickerIcon, contentIcon;
		CharSequence tickerText, contentTitle, contentText, contentTime;
		final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
		contentIcon = android.R.drawable.stat_notify_error;
		final Context context = WeatherNotificationService.this;
		contentTime = df.format(new Date());
		long when = (new Date()).getTime();

		if (e instanceof NoLocationException) {
			setShortAlarm();
			tickerText = WeatherNotificationService.this
					.getString(R.string.location_error);
			contentTitle = WeatherNotificationService.this
					.getString(R.string.location_error);
		} else {
			if (e instanceof NetworkErrorException)
				updateAlarm();
			else
				setShortAlarm();

			// Network error has occurred
			// Set title
			tickerText = WeatherNotificationService.this
					.getString(R.string.download_error);
			contentTitle = WeatherNotificationService.this
					.getString(R.string.download_error);
		}

		final Date lastTime = WeatherNotificationSettings
				.getLastUpdateTime(WeatherNotificationService.this);
		Float temperatureF = null;
		if (lastTime != null) {
			temperatureF = Float.parseFloat(WeatherNotificationSettings
					.getSavedLastTemperature(context));
			contentText = String.format("%s %.1f °C %s %s",
					context.getString(R.string.last_temperature), temperatureF,
					context.getString(R.string._tid_), df.format(lastTime));
			tickerIcon = TempToDrawable.getDrawableFromTemp(temperatureF);

		} else {
			contentText = WeatherNotificationService.this
					.getString(R.string.press_for_update);
			tickerIcon = android.R.drawable.stat_notify_error;
		}

		makeNotification(tickerIcon, contentIcon, tickerText, contentTitle,
				contentText, contentTime, when, temperatureF);

	}

	/**
	 * Make notification and post it to the NotificationManager
	 * 
	 * @param tickerIcon
	 *            Icon shown in notification bar
	 * @param contentIcon
	 *            Icon shown in notification
	 * @param tickerText
	 *            Text shown in notification bar
	 * @param contentTitle
	 *            Title shown in notification
	 * @param contentText
	 *            Description shown in notification
	 * @param contentTime
	 *            Time shown in notification
	 * @param when2
	 */
	private void makeNotification(int tickerIcon, int contentIcon,
			CharSequence tickerText, CharSequence contentTitle,
			CharSequence contentText, CharSequence contentTime, long when2,
			Float temperature) {
		final long when = System.currentTimeMillis();
		// Make notification
		Notification notification = null;

		final Intent notificationIntent = new Intent(
				WeatherNotificationService.this,
				WeatherNotificationService.class);
		final PendingIntent contentIntent = PendingIntent.getService(
				WeatherNotificationService.this, 0, notificationIntent, 0);

		// Check if Notification.Builder exists (11+)
		if (Build.VERSION.SDK_INT >= 11) {
			// Honeycomb ++
			NotificationBuilder builder = new NotificationBuilder(this);
			builder.setAutoCancel(false);
			builder.setContentTitle(contentTitle);
			builder.setContentText(contentText);
			builder.setTicker(tickerText);
			builder.setWhen(when2);
			builder.setSmallIcon(tickerIcon);
			builder.setOngoing(true);
			builder.setContentIntent(contentIntent);
			if (temperature != null)
				builder.makeContentView(contentTitle, contentText, when2,
						temperature, tickerIcon);
			notification = builder.getNotification();
		} else {
			// Gingerbread --
			notification = new Notification(tickerIcon, tickerText, when);
			notification.flags = Notification.FLAG_ONGOING_EVENT;

			final RemoteViews contentView = new RemoteViews(getPackageName(),
					R.layout.weathernotification);
			contentView.setImageViewResource(R.id.icon, contentIcon);
			contentView.setTextViewText(R.id.title, contentTitle);
			contentView.setTextViewText(R.id.title, contentTitle);
			contentView.setTextViewText(R.id.text, contentText);
			contentView.setTextViewText(R.id.time, contentTime);
			notification.contentView = contentView;
		}
		notification.contentIntent = contentIntent;

		// Post notification
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(1, notification);
	}

	/**
	 * Display a notification with temperature
	 * 
	 * @param weather
	 *            Weather element to with data to be shown in notification, if
	 *            null a message for that say that this station does not provide
	 *            data will be shown
	 */
	private void makeNotification(WeatherElement weather) {
		int tickerIcon, contentIcon;
		CharSequence tickerText, contentTitle, contentText, contentTime;
		final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
		long when;
		Float temperatureF = null;
		if (weather != null) {
			// Has data
			final WeatherElement temperature = weather;
			// Find name
			final String stationName = WeatherNotificationSettings
					.getStationName(WeatherNotificationService.this);

			// Find icon
			tickerIcon = TempToDrawable.getDrawableFromTemp(Float
					.valueOf(temperature.getValue()));
			contentIcon = tickerIcon;
			// Set title
			tickerText = stationName;
			contentTitle = stationName;
			contentTime = df.format(temperature.getDate());
			when = temperature.getDate().getTime();

			final Context context = WeatherNotificationService.this;
			temperatureF = new Float(temperature.getValue());
			contentText = String.format("%s %.1f °C", context
					.getString(R.string.temperatur_),
					new Float(temperature.getValue()));

			updateAlarm(weather);

		} else {
			// No data
			contentIcon = android.R.drawable.stat_notify_error;
			final Context context = WeatherNotificationService.this;
			contentTime = df.format(new Date());
			when = (new Date()).getTime();
			tickerText = context.getText(R.string.no_available_data);
			contentTitle = context.getText(R.string.no_available_data);
			contentText = context.getString(R.string.try_another_station);
			tickerIcon = android.R.drawable.stat_notify_error;

		}

		makeNotification(tickerIcon, contentIcon, tickerText, contentTitle,
				contentText, contentTime, when, temperatureF);

	}

	@Override
	protected void onHandleIntent(Intent intent) {
		switch (intent.getIntExtra(INTENT_EXTRA_ACTION,
				INTENT_EXTRA_ACTION_GET_TEMP)) {
		case INTENT_EXTRA_ACTION_GET_TEMP:
			showTemp();
			break;
		case INTENT_EXTRA_ACTION_UPDATE_ALARM:
			updateAlarm();
			break;
		default:
			showTemp();
			break;
		}
	}

	private void removeAlarm() {
		final PendingIntent pendingIntent = PendingIntent.getService(this, 0,
				new Intent(this, WeatherNotificationService.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		final AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pendingIntent);
	}

	/**
	 * Set alarm
	 * 
	 * @param updateRate
	 *            in minutes
	 */
	private void setAlarm(long updateRate) {
		// Set alarm
		final PendingIntent pendingIntent = PendingIntent.getService(this, 0,
				new Intent(this, WeatherNotificationService.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		final AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		// Set time to next hour plus 5 minutes:
		final long now = System.currentTimeMillis();
		long triggerAtTime = now;
		// set back to last hour plus 2 minutes:
		triggerAtTime -= triggerAtTime % 3600000 - 12000;
		// Add selected update rate
		triggerAtTime += updateRate * 60000;

		// Check that trigger time is not passed.
		if (triggerAtTime < now)
			triggerAtTime = now + updateRate * 60000;

		alarm.set(AlarmManager.RTC, triggerAtTime, pendingIntent);
	}

	private void setShortAlarm() {
		final int updateRate = WeatherNotificationSettings.getUpdateRate(this);
		if (updateRate > 0)
			setAlarm(10);
	}

	/**
	 * Update the temperature
	 */
	void showTemp() {
		// Stops
		if (!CheckInternetStatus.canConnectToInternet(this)) {
			addConnectionChangedReceiver();
			makeNotification(new NetworkErrorException());
		}

		try {
			final WeatherElement returnValue = getWeatherData();
			makeNotification(returnValue);
		} catch (final NetworkErrorException e) {
			makeNotification(e);
		} catch (final HttpException e) {
			makeNotification(e);
		} catch (final NoLocationException e) {
			makeNotification(e);
		}

	}

	/**
	 * Set alarm for next time the notification should be updated
	 */
	private void updateAlarm() {
		// Find update rate

		final int updateRate = WeatherNotificationSettings.getUpdateRate(this);

		if (updateRate <= 0) {
			removeAlarm();
			return;
		} else
			setAlarm(updateRate);
	}

	private void updateAlarm(WeatherElement weather) {
		final int updateRate = WeatherNotificationSettings.getUpdateRate(this);

		if (updateRate <= 0) {
			removeAlarm();
			return;
		} else {
			// Check if weather element is more than 1 hour old
			long tooOldTime = (new Date()).getTime() - 1000 * 3600;
			if (weather.getTime() > tooOldTime)
				setAlarm(updateRate);
			else
				setShortAlarm();
		}
	}

}
