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

import no.firestorm.R;
import android.app.Notification;
import android.content.Context;
import android.widget.RemoteViews;

/**
 * Builder class for {@link Notification} objects. Allows easier control over
 * all the flags, as well as help constructing the typical notification layouts.
 */
public class NotificationBuilder extends Notification.Builder {
	Context mContext;

	public NotificationBuilder(Context context) {
		super(context);
		mContext = context;
	}

	public NotificationBuilder makeContentView(CharSequence contentTitle,
			CharSequence contentText, long when, float temperature, int icon) {
		RemoteViews contentView = new RemoteViews(mContext.getPackageName(),
				R.layout.status_bar_latest_event_content);
		//contentView.setImageViewResource(R.id.icon, icon);
		String temp = String.format("%.1f °C", temperature);
		contentView.setTextViewText(R.id.icon, temp);
		contentView.setTextViewText(R.id.title, contentTitle);
		//contentView.setTextViewText(R.id.text, contentText);
		contentView.setLong(R.id.time, "setTime", when);	
		this.setContent(contentView);
		return this;
	}
}
