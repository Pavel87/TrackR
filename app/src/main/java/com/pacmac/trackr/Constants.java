package com.pacmac.trackr;

/**
 * Created by tqm837 on 4/27/2016.
 */
public class Constants {

    public static final String TAG = "TrackR";
    public static final String KEY_LATITUDE = "latitude_key";
    public static final String KEY_LONGITUDE = "longitude_key";
    public static final String KEY_TIMESTAMP = "timestamp_key";
    public static final String KEY_ADDRESS = "address_key";
    public static final String PREF_TRACKR = "-tracker_PREF";
    public static final String TRACKING_STATE = "trackr_gps_state";
    public static final String TRACKING_ID = "trackr_gps_ID";
    public static final String RECEIVING_ID = "receiver_gps_ID";
    public static final String PACKAGE_NAME = "com.pacmac.trackr";
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String FIRST_RUN = "firstRUN";
    public static final String PADLOCK_ACTIVE = "padlock_active";
    public static final String PADLOCK_PASS = "padlock_pass";
    public static final int SUCCESS = 0;
    public static final int ERROR = 134;
    public static final int TYPE_TRACKING_ID = 0;
    public static final int TYPE_RECEIVING_ID = 1;
    public static final int TYPE_PASSWORD_ACTIVE = 2;
    public static final int TYPE_PASSWORD_NOT_ACTIVE = 3;
    public final static int TIME_BATTERY_OK = 30 * 60 * 1000;
    public final static int TIME_BATTERY_LOW = 60 * 60 * 1000;



}
