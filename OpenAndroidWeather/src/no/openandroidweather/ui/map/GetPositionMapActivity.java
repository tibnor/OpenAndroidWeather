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

package no.openandroidweather.ui.map;

import java.util.List;

import no.openandroidweather.R;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class GetPositionMapActivity extends MapActivity {
	protected static final String TAG = "GetPositionMapActivity";
	public static final String POSITION = "position";
	private MapView mMapView;

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_get_position);
		final MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mMapView = mapView;
		
		final List<Overlay> overlays = mapView.getOverlays();
		overlays.clear();
		overlays.add(new MapGestureDetectorOverlay());
		
		// Set result to canceled in case of canceling 
		setResult(RESULT_CANCELED);
		
		// Show help toast
		Toast.makeText(this, R.string.GetPositionMapHint, Toast.LENGTH_LONG).show();

	}
	
	private void onLongPress(MotionEvent e){
		MapView mapView = mMapView;
		int xMin = mapView.getLeft();
		int xSpan = mapView.getWidth();		
		int yMin = mapView.getTop();
		int ySpan = mapView.getHeight();
		
		// Get offset from center:
		float x = e.getX()-xMin - xSpan/2;
		float y = e.getY()-yMin - ySpan/2;
		
		int longSpan = mapView.getLongitudeSpan();
		int latSpan = mapView.getLatitudeSpan();
		GeoPoint center = mapView.getMapCenter();
		
		
		Location loc = new Location("");
		loc.setLatitude((center.getLatitudeE6()-y/ySpan*latSpan)/1e6);
		loc.setLongitude((center.getLongitudeE6()+x/xSpan*longSpan)/1e6);
		
		// Set result:

		Intent data = new Intent();
		data.putExtra(POSITION, loc);
		setResult(RESULT_OK, data );
		finish();
	}

	public class MapGestureDetectorOverlay extends Overlay implements
			OnGestureListener {
		private final GestureDetector gestureDetector = new GestureDetector(
				this);

		@Override
		public boolean onDown(final MotionEvent e) {
			return false;
		}

		@Override
		public boolean onFling(final MotionEvent e1, final MotionEvent e2,
				final float velocityX, final float velocityY) {
			return false;
		}

		@Override
		public void onLongPress(final MotionEvent e) {
			GetPositionMapActivity.this.onLongPress(e);
		}

		@Override
		public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
				final float distanceX, final float distanceY) {
			return false;
		}

		@Override
		public void onShowPress(final MotionEvent e) {
		}

		@Override
		public boolean onSingleTapUp(final MotionEvent e) {
			return false;
		}

		@Override
		public boolean onTouchEvent(final MotionEvent event,
				final MapView mapView) {
			if (gestureDetector.onTouchEvent(event))
				return true;
			return false;
		}

	}
}
