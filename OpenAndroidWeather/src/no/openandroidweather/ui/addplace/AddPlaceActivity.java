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

package no.openandroidweather.ui.addplace;

import no.openandroidweather.R;
import no.openandroidweather.ui.map.GetPositionMapActivity;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider;
import no.openandroidweather.weathercontentprovider.WeatherContentProvider.Place;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddPlaceActivity extends Activity {
	public final String RETURN_URI = "uri";
	EditText mPlaceName, mLongtitude, mLatitude, mAltitude;

	protected void addPlace() {
		if (checkAllInputs()) {
			final ContentValues values = new ContentValues();
			values.put(Place.ALTITUDE, new Double(mAltitude.getText()
					.toString()));
			values.put(Place.LATITUDE, new Double(mLatitude.getText()
					.toString()));
			values.put(Place.LONGITUDE, new Double(mLongtitude.getText()
					.toString()));
			values.put(Place.PLACE_NAME, mPlaceName.getText().toString());
			final ContentResolver cr = getContentResolver();
			final Uri url = Uri.withAppendedPath(
					WeatherContentProvider.CONTENT_URI, Place.CONTENT_PATH);
			final Uri uri = cr.insert(url, values);

			final Intent data = new Intent();
			data.putExtra(RETURN_URI, uri);
			setResult(RESULT_OK, data);
			finish();
		}

	}

	/**
	 * Check that all inputs are valid.
	 */
	private boolean checkAllInputs() {
		final String floatRegEx = "^[-+]?[0-9]*\\.?[0-9]+$";
		if (mAltitude.getText().length() == 0
				|| mLatitude.getText().length() == 0
				|| mLongtitude.getText().length() == 0
				|| mPlaceName.getText().length() == 0) {
			Toast.makeText(this, R.string.Add_place_required_fields,
					Toast.LENGTH_LONG).show();
			return false;
		} else if (!mAltitude.getText().toString().matches(floatRegEx)
				|| !mLongtitude.getText().toString().matches(floatRegEx)
				|| !mLatitude.getText().toString().matches(floatRegEx)) {
			Toast.makeText(this, R.string.Add_place_required_format,
					Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	protected void getCurrentPosition() {
		final Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final String provider = locationManager.getBestProvider(criteria, true);
		if (provider == null) {
			showMessage(R.string.no_position);
			return;
		}

		final Location loc = locationManager.getLastKnownLocation(provider);
		if (loc == null) {
			showMessage(R.string.no_position);
			return;
		}

		renderLocation(loc);
	}

	protected void getPositionFromMap() {
		startActivityForResult(new Intent(this, GetPositionMapActivity.class),
				1);
	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			final Location loc = data.getExtras().getParcelable(
					GetPositionMapActivity.POSITION);
			if (loc != null)
				renderLocation(loc);
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_place);

		setResult(RESULT_CANCELED);

		final Button getCurrentPositionButton = (Button) findViewById(R.id.add_this_position_button);
		getCurrentPositionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				getCurrentPosition();
			}
		});

		final Button getPositionFromMapButton = (Button) findViewById(R.id.get_position_from_map);
		getPositionFromMapButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				getPositionFromMap();
			}
		});

		final Button addPlaceButton = (Button) findViewById(R.id.add_place);
		addPlaceButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				addPlace();
			}
		});

		mPlaceName = (EditText) findViewById(R.id.place_name);
		mLongtitude = (EditText) findViewById(R.id.longitude);
		mLatitude = (EditText) findViewById(R.id.latitude);
		mAltitude = (EditText) findViewById(R.id.altitude);

	}

	/**
	 * @param location
	 *            to render on the view
	 */
	private void renderLocation(final Location location) {
		mLatitude.setText(location.getLatitude() + "");
		mLongtitude.setText(location.getLongitude() + "");
		if (location.hasAltitude())
			mAltitude.setText(location.getAltitude() + "");
	}

	/**
	 * Show a message
	 */
	private void showMessage(final int resId) {
		final Toast toast = new Toast(this);
		toast.setText(resId);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();
	}
}
