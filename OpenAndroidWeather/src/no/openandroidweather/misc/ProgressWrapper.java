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

package no.openandroidweather.misc;

public class ProgressWrapper implements IProgressItem {

	private final int min;
	private final int max;
	private final IProgressItem mListener;

	public ProgressWrapper(int min, int max, IProgressItem mListener) {
		super();
		this.min = min;
		this.max = max;
		this.mListener = mListener;
	}

	@Override
	public void progress(int progress) {
		mListener.progress((int) (min + progress / 1000. * (max - min)));
	}

}
