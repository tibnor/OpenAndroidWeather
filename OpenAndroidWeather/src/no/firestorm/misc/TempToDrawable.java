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

package no.firestorm.misc;

import java.util.HashMap;
import java.util.Map;

import android.os.Build;

import no.firestorm.R;

/**
 * Convert temperature to desired icon
 */
public final class TempToDrawable {
	private static final Map<Float, Integer> tempDrawable = new HashMap<Float, Integer>();

	static {
		if (Build.VERSION.SDK_INT < 11) {
			tempDrawable.put(-55.0f, R.drawable.t_55);
			tempDrawable.put(-54.0f, R.drawable.t_54);
			tempDrawable.put(-53.0f, R.drawable.t_53);
			tempDrawable.put(-52.0f, R.drawable.t_52);
			tempDrawable.put(-51.0f, R.drawable.t_51);
			tempDrawable.put(-50.0f, R.drawable.t_50);
			tempDrawable.put(-49.0f, R.drawable.t_49);
			tempDrawable.put(-48.0f, R.drawable.t_48);
			tempDrawable.put(-47.0f, R.drawable.t_47);
			tempDrawable.put(-46.0f, R.drawable.t_46);
			tempDrawable.put(-45.0f, R.drawable.t_45);
			tempDrawable.put(-44.0f, R.drawable.t_44);
			tempDrawable.put(-43.0f, R.drawable.t_43);
			tempDrawable.put(-42.0f, R.drawable.t_42);
			tempDrawable.put(-41.0f, R.drawable.t_41);
			tempDrawable.put(-40.0f, R.drawable.t_40);
			tempDrawable.put(-39.0f, R.drawable.t_39);
			tempDrawable.put(-38.0f, R.drawable.t_38);
			tempDrawable.put(-37.0f, R.drawable.t_37);
			tempDrawable.put(-36.0f, R.drawable.t_36);
			tempDrawable.put(-35.0f, R.drawable.t_35);
			tempDrawable.put(-34.0f, R.drawable.t_34);
			tempDrawable.put(-33.0f, R.drawable.t_33);
			tempDrawable.put(-32.0f, R.drawable.t_32);
			tempDrawable.put(-31.0f, R.drawable.t_31);
			tempDrawable.put(-30.0f, R.drawable.t_30);
			tempDrawable.put(-29.0f, R.drawable.t_29);
			tempDrawable.put(-28.0f, R.drawable.t_28);
			tempDrawable.put(-27.0f, R.drawable.t_27);
			tempDrawable.put(-26.0f, R.drawable.t_26);
			tempDrawable.put(-25.0f, R.drawable.t_25);
			tempDrawable.put(-24.0f, R.drawable.t_24);
			tempDrawable.put(-23.0f, R.drawable.t_23);
			tempDrawable.put(-22.0f, R.drawable.t_22);
			tempDrawable.put(-21.0f, R.drawable.t_21);
			tempDrawable.put(-20.0f, R.drawable.t_20);
			tempDrawable.put(-19.0f, R.drawable.t_19);
			tempDrawable.put(-18.0f, R.drawable.t_18);
			tempDrawable.put(-17.0f, R.drawable.t_17);
			tempDrawable.put(-16.0f, R.drawable.t_16);
			tempDrawable.put(-15.0f, R.drawable.t_15);
			tempDrawable.put(-14.0f, R.drawable.t_14);
			tempDrawable.put(-13.0f, R.drawable.t_13);
			tempDrawable.put(-12.0f, R.drawable.t_12);
			tempDrawable.put(-11.0f, R.drawable.t_11);
			tempDrawable.put(-10.0f, R.drawable.t_10);
			tempDrawable.put(-9.0f, R.drawable.t_9);
			tempDrawable.put(-8.0f, R.drawable.t_8);
			tempDrawable.put(-7.0f, R.drawable.t_7);
			tempDrawable.put(-6.0f, R.drawable.t_6);
			tempDrawable.put(-5.0f, R.drawable.t_5);
			tempDrawable.put(-4.0f, R.drawable.t_4);
			tempDrawable.put(-3.0f, R.drawable.t_3);
			tempDrawable.put(-2.0f, R.drawable.t_2);
			tempDrawable.put(-1.0f, R.drawable.t_1);
			tempDrawable.put(0.0f, R.drawable.t0);
			tempDrawable.put(1.0f, R.drawable.t1);
			tempDrawable.put(2.0f, R.drawable.t2);
			tempDrawable.put(3.0f, R.drawable.t3);
			tempDrawable.put(4.0f, R.drawable.t4);
			tempDrawable.put(5.0f, R.drawable.t5);
			tempDrawable.put(6.0f, R.drawable.t6);
			tempDrawable.put(7.0f, R.drawable.t7);
			tempDrawable.put(8.0f, R.drawable.t8);
			tempDrawable.put(9.0f, R.drawable.t9);
			tempDrawable.put(10.0f, R.drawable.t10);
			tempDrawable.put(11.0f, R.drawable.t11);
			tempDrawable.put(12.0f, R.drawable.t12);
			tempDrawable.put(13.0f, R.drawable.t13);
			tempDrawable.put(14.0f, R.drawable.t14);
			tempDrawable.put(15.0f, R.drawable.t15);
			tempDrawable.put(16.0f, R.drawable.t16);
			tempDrawable.put(17.0f, R.drawable.t17);
			tempDrawable.put(18.0f, R.drawable.t18);
			tempDrawable.put(19.0f, R.drawable.t19);
			tempDrawable.put(20.0f, R.drawable.t20);
			tempDrawable.put(21.0f, R.drawable.t21);
			tempDrawable.put(22.0f, R.drawable.t22);
			tempDrawable.put(23.0f, R.drawable.t23);
			tempDrawable.put(24.0f, R.drawable.t24);
			tempDrawable.put(25.0f, R.drawable.t25);
			tempDrawable.put(26.0f, R.drawable.t26);
			tempDrawable.put(27.0f, R.drawable.t27);
			tempDrawable.put(28.0f, R.drawable.t28);
			tempDrawable.put(29.0f, R.drawable.t29);
			tempDrawable.put(30.0f, R.drawable.t30);
			tempDrawable.put(31.0f, R.drawable.t31);
			tempDrawable.put(32.0f, R.drawable.t32);
			tempDrawable.put(33.0f, R.drawable.t33);
			tempDrawable.put(34.0f, R.drawable.t34);
			tempDrawable.put(35.0f, R.drawable.t35);
			tempDrawable.put(36.0f, R.drawable.t36);
			tempDrawable.put(37.0f, R.drawable.t37);
			tempDrawable.put(38.0f, R.drawable.t38);
			tempDrawable.put(39.0f, R.drawable.t39);
			tempDrawable.put(40.0f, R.drawable.t40);
			tempDrawable.put(41.0f, R.drawable.t41);
			tempDrawable.put(42.0f, R.drawable.t42);
			tempDrawable.put(43.0f, R.drawable.t43);
			tempDrawable.put(44.0f, R.drawable.t44);
			tempDrawable.put(45.0f, R.drawable.t45);
			tempDrawable.put(46.0f, R.drawable.t46);
			tempDrawable.put(47.0f, R.drawable.t47);
			tempDrawable.put(48.0f, R.drawable.t48);
			tempDrawable.put(49.0f, R.drawable.t49);
			tempDrawable.put(50.0f, R.drawable.t50);
		} else {
			tempDrawable.put(-55.0f, R.drawable.tn_55);
			tempDrawable.put(-54.0f, R.drawable.tn_54);
			tempDrawable.put(-53.0f, R.drawable.tn_53);
			tempDrawable.put(-52.0f, R.drawable.tn_52);
			tempDrawable.put(-51.0f, R.drawable.tn_51);
			tempDrawable.put(-50.0f, R.drawable.tn_50);
			tempDrawable.put(-49.0f, R.drawable.tn_49);
			tempDrawable.put(-48.0f, R.drawable.tn_48);
			tempDrawable.put(-47.0f, R.drawable.tn_47);
			tempDrawable.put(-46.0f, R.drawable.tn_46);
			tempDrawable.put(-45.0f, R.drawable.tn_45);
			tempDrawable.put(-44.0f, R.drawable.tn_44);
			tempDrawable.put(-43.0f, R.drawable.tn_43);
			tempDrawable.put(-42.0f, R.drawable.tn_42);
			tempDrawable.put(-41.0f, R.drawable.tn_41);
			tempDrawable.put(-40.0f, R.drawable.tn_40);
			tempDrawable.put(-39.0f, R.drawable.tn_39);
			tempDrawable.put(-38.0f, R.drawable.tn_38);
			tempDrawable.put(-37.0f, R.drawable.tn_37);
			tempDrawable.put(-36.0f, R.drawable.tn_36);
			tempDrawable.put(-35.0f, R.drawable.tn_35);
			tempDrawable.put(-34.0f, R.drawable.tn_34);
			tempDrawable.put(-33.0f, R.drawable.tn_33);
			tempDrawable.put(-32.0f, R.drawable.tn_32);
			tempDrawable.put(-31.0f, R.drawable.tn_31);
			tempDrawable.put(-30.0f, R.drawable.tn_30);
			tempDrawable.put(-29.0f, R.drawable.tn_29);
			tempDrawable.put(-28.0f, R.drawable.tn_28);
			tempDrawable.put(-27.0f, R.drawable.tn_27);
			tempDrawable.put(-26.0f, R.drawable.tn_26);
			tempDrawable.put(-25.0f, R.drawable.tn_25);
			tempDrawable.put(-24.0f, R.drawable.tn_24);
			tempDrawable.put(-23.0f, R.drawable.tn_23);
			tempDrawable.put(-22.0f, R.drawable.tn_22);
			tempDrawable.put(-21.0f, R.drawable.tn_21);
			tempDrawable.put(-20.0f, R.drawable.tn_20);
			tempDrawable.put(-19.0f, R.drawable.tn_19);
			tempDrawable.put(-18.0f, R.drawable.tn_18);
			tempDrawable.put(-17.0f, R.drawable.tn_17);
			tempDrawable.put(-16.0f, R.drawable.tn_16);
			tempDrawable.put(-15.0f, R.drawable.tn_15);
			tempDrawable.put(-14.0f, R.drawable.tn_14);
			tempDrawable.put(-13.0f, R.drawable.tn_13);
			tempDrawable.put(-12.0f, R.drawable.tn_12);
			tempDrawable.put(-11.0f, R.drawable.tn_11);
			tempDrawable.put(-10.0f, R.drawable.tn_10);
			tempDrawable.put(-9.0f, R.drawable.tn_9);
			tempDrawable.put(-8.0f, R.drawable.tn_8);
			tempDrawable.put(-7.0f, R.drawable.tn_7);
			tempDrawable.put(-6.0f, R.drawable.tn_6);
			tempDrawable.put(-5.0f, R.drawable.tn_5);
			tempDrawable.put(-4.0f, R.drawable.tn_4);
			tempDrawable.put(-3.0f, R.drawable.tn_3);
			tempDrawable.put(-2.0f, R.drawable.tn_2);
			tempDrawable.put(-1.0f, R.drawable.tn_1);
			tempDrawable.put(0.0f, R.drawable.tn0);
			tempDrawable.put(1.0f, R.drawable.tn1);
			tempDrawable.put(2.0f, R.drawable.tn2);
			tempDrawable.put(3.0f, R.drawable.tn3);
			tempDrawable.put(4.0f, R.drawable.tn4);
			tempDrawable.put(5.0f, R.drawable.tn5);
			tempDrawable.put(6.0f, R.drawable.tn6);
			tempDrawable.put(7.0f, R.drawable.tn7);
			tempDrawable.put(8.0f, R.drawable.tn8);
			tempDrawable.put(9.0f, R.drawable.tn9);
			tempDrawable.put(10.0f, R.drawable.tn10);
			tempDrawable.put(11.0f, R.drawable.tn11);
			tempDrawable.put(12.0f, R.drawable.tn12);
			tempDrawable.put(13.0f, R.drawable.tn13);
			tempDrawable.put(14.0f, R.drawable.tn14);
			tempDrawable.put(15.0f, R.drawable.tn15);
			tempDrawable.put(16.0f, R.drawable.tn16);
			tempDrawable.put(17.0f, R.drawable.tn17);
			tempDrawable.put(18.0f, R.drawable.tn18);
			tempDrawable.put(19.0f, R.drawable.tn19);
			tempDrawable.put(20.0f, R.drawable.tn20);
			tempDrawable.put(21.0f, R.drawable.tn21);
			tempDrawable.put(22.0f, R.drawable.tn22);
			tempDrawable.put(23.0f, R.drawable.tn23);
			tempDrawable.put(24.0f, R.drawable.tn24);
			tempDrawable.put(25.0f, R.drawable.tn25);
			tempDrawable.put(26.0f, R.drawable.tn26);
			tempDrawable.put(27.0f, R.drawable.tn27);
			tempDrawable.put(28.0f, R.drawable.tn28);
			tempDrawable.put(29.0f, R.drawable.tn29);
			tempDrawable.put(30.0f, R.drawable.tn30);
			tempDrawable.put(31.0f, R.drawable.tn31);
			tempDrawable.put(32.0f, R.drawable.tn32);
			tempDrawable.put(33.0f, R.drawable.tn33);
			tempDrawable.put(34.0f, R.drawable.tn34);
			tempDrawable.put(35.0f, R.drawable.tn35);
			tempDrawable.put(36.0f, R.drawable.tn36);
			tempDrawable.put(37.0f, R.drawable.tn37);
			tempDrawable.put(38.0f, R.drawable.tn38);
			tempDrawable.put(39.0f, R.drawable.tn39);
			tempDrawable.put(40.0f, R.drawable.tn40);
			tempDrawable.put(41.0f, R.drawable.tn41);
			tempDrawable.put(42.0f, R.drawable.tn42);
			tempDrawable.put(43.0f, R.drawable.tn43);
			tempDrawable.put(44.0f, R.drawable.tn44);
			tempDrawable.put(45.0f, R.drawable.tn45);
			tempDrawable.put(46.0f, R.drawable.tn46);
			tempDrawable.put(47.0f, R.drawable.tn47);
			tempDrawable.put(48.0f, R.drawable.tn48);
			tempDrawable.put(49.0f, R.drawable.tn49);
			tempDrawable.put(50.0f, R.drawable.tn50);
		}
	}

	/**
	 * @param temp
	 *            Temperature to be displayed
	 * @return icon with desired temperature
	 */
	public static int getDrawableFromTemp(float temp) {
		return tempDrawable.get((float) Math.round(temp));
	}
}
