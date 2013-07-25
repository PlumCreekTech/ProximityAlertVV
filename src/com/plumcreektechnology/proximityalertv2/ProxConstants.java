package com.plumcreektechnology.proximityalertv2;

public interface ProxConstants {
	public static final String PROX_ALERT_INTENT = "com.plumcreektechnology.proximityalertv2.ProxReceiver";
	public static final String PACKAGE = "com.plumcreektechnology.proximityalertv2";
	/*
	 * Invalid values, used to test geofence storage when retrieving geofences
	 */
	public static final long INVALID_LONG_VALUE = -999l;
	public static final float INVALID_FLOAT_VALUE = -999.0f;
	public static final double INVALID_DOUBLE_VALUE = -999.0d;
	public static final int INVALID_INT_VALUE = -999;
	/*
	 * MyGeofence constants
	 */
	public static final float RADIUS = 200;
	public static final long EXPIRATION = -1;
	public static final String URL = "https://twitter.com/StealthMountain";
	public static final int ICON = R.drawable.ic_launcher;
}
