package com.pacmac.trackr;

import android.Manifest;

/**
 * Created by tqm837 on 4/27/2016.
 */
public class Constants {

    public static final String TAG = "TrackR";
    public static final String RATING_POPUP_COUNTER = "rating_popup_counter";
    public static final String RATING_POPUP_ENABLED = "rating_popup_enabled";
    public static final String KEY_LATITUDE = "latitude_key";
    public static final String KEY_LONGITUDE = "longitude_key";
    public static final String KEY_ITEM_ORDER = "rec_id_order";
    public static final String KEY_POSIION = "position_of_selected_device";
    public static final String KEY_ALIAS_ARRAY = "alias_array_for_map";
    public static final String PREF_TRACKR = "-tracker_PREF";
    public static final String NEW_UPDATE = "TrackR_Update";
    public static final String MY_PHONE_IN_LIST = "trackr_show_this_phone_in_list";
    public static final String TRACKING_STATE = "trackr_gps_state";
    public static final String TRACKING_FREQ = "trackr_update_freq";
    public static final String TRACKING_ID = "trackr_gps_ID";
    public static final String TRACKING_ID_RAW = "trackr_gps_ID_raw";
    public static final String RECEIVING_ID_RAW = "receiver_gps_ID_raw";
    public static final String RECEIVING_ID = "receiver_gps_ID";
    public static final String RECEIVING_ID_CHANGE = "receiver_gps_ID_changed";
    public static final String PACKAGE_NAME = "com.pacmac.trackr";
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String FIRST_RUN = "firstRUN";
    public static final String PADLOCK_ACTIVE = "padlock_active";
    public static final String PADLOCK_PASS = "padlock_pass";

    public static final String JSON_REC_IDS_FILE_NAME = "/recIdsJson";
    public static final String JSON_LOC_FILE_NAME = "/locRecsJson";

    public final static String REG_KEY = "ieg9ioa9qhlbnff2714e6s1a8n";

    public static final int TYPE_PASSWORD_ACTIVE = 2;
    public static final int TYPE_PASSWORD_NOT_ACTIVE = 3;
    public final static int TIME_BATTERY_OK = 20; // 20 * 60 * 1000;
    public final static int FB_REQUEST_TIMEOUT = 5*60*1000; // 5 minutes
    public final static int OLD_ID_THRESHOLD = 7*24 * 60 * 60 * 1000; // 7 days
    public final static int RATING_POPUP_ATTEMPTS = 7; // popup will show after user open the app 8 times

    public static final int TYPE_NORMAL = 0;


    public static final String ADDRESS_RESOLVER_ACTION = "TRACKR_ADDRESS_RESOLVER_ACTION";
    public static final String ADDRESS_RESOLVER_ADDRESS = "TRACKR_ADDRESS_RESOLVER_ADDRESS";
    public static final String ADDRESS_RESOLVER_ROWID = "TRACKR_ADDRESS_RESOLVER_ROWID";


    protected static final int SETTINGS_REQUESTCODE = 8887;
    protected static final int SETTINGS_UPDATE_RESULT = 8890;

    protected static final int EDIT_IMAGE_REQUEST_CODE = 8889;
    protected static final int EDIT_RESULT_REQUEST_CODE = 8888;
    public static final int EDIT_DELETE_POSITION = -8888;
    public static final String EDIT_USER_ALIAS = "TRACKR_EDIT_USER_ALIAS";
    public static final String EDIT_USER_IMG = "TRACKR_EDIT_USER_IMG";
    public static final String EDIT_USER_ID = "TRACKR_EDIT_USER_ID";
    public static final String EDIT_USER_POSITION = "TRACKR_EDIT_USER_POSITION";
    public static final String EDIT_USER_TYPE = "TRACKR_EDIT_USER_TYPE";
    public static final String MY_PHONE_ALIAS = "TRACKR_MY_PHONE_ALIAS";
    public static final String MY_PHONE_IMG = "TRACKR_MY_PHONE_IMG";

    public static final String PERMISSION_ENABLED = "TRACKR_PERMISSION_ENABLED";
    public static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;



}
