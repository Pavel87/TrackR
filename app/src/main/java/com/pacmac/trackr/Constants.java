package com.pacmac.trackr;

/**
 * Created by tqm837 on 4/27/2016.
 */
public class Constants {

    public static final String TAG = "TrackR";
    public static final String RATING_POPUP_COUNTER = "rating_popup_counter";
    public static final String RATING_POPUP_ENABLED = "rating_popup_enabled";
    public static final String KEY_ID = "id_record_key";
    public static final String KEY_LATITUDE = "latitude_key";
    public static final String KEY_LONGITUDE = "longitude_key";
    public static final String KEY_TIMESTAMP = "timestamp_key";
    public static final String KEY_ITEM_ORDER = "rec_id_order";
    public static final String KEY_POSIION = "position_of_selected_device";
    public static final String KEY_ALIAS_ARRAY = "alias_array_for_map";
    public static final String PREF_TRACKR = "-tracker_PREF";
    public static final String NEW_UPDATE = "TrackR_Update";
    public static final String TRACKING_STATE = "trackr_gps_state";
    public static final String TRACKING_ID = "trackr_gps_ID";
    public static final String TRACKING_ID_RAW = "trackr_gps_ID_raw";
    public static final String RECEIVING_ID_CHANGE = "receiver_gps_ID_changed";
    public static final String PACKAGE_NAME = "com.pacmac.trackr";
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String FIRST_RUN = "firstRUN";
    public static final String PADLOCK_ACTIVE = "padlock_active";
    public static final String PADLOCK_PASS = "padlock_pass";

    public static final String REMOTE_USER_ID = "user_id";
    public static final String REMOTE_ADDRESS = "user_address";

    public static final String JSON_REC_IDS_FILE_NAME = "/recIdsJson";
    public static final String JSON_LOC_FILE_NAME = "/locRecsJson";

    public static final int SUCCESS = 0;
    public static final int ERROR = 134;
    public static final int TYPE_TRACKING_ID = 0;
    public static final int TYPE_RECEIVING_ID = 1;
    public static final int TYPE_PASSWORD_ACTIVE = 2;
    public static final int TYPE_PASSWORD_NOT_ACTIVE = 3;
    public final static int UPDATE_TIMEOUT = 10 * 60 * 1000;
    public final static int TIME_BATTERY_OK = 20 * 60 * 1000;
    public final static int TIME_BATTERY_LOW = 45 * 60 * 1000;
    public final static int OLD_ID_THRESHOLD = 7*24 * 60 * 60 * 1000; // 7 days
    public final static int RATING_POPUP_ATTEMPTS = 8; // popup will show after user open the app 8 times

    public static final int TYPE_HEADER = -1;
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_TRACKID = 1;
    public static final int TYPE_TRACK_SWITCH = 2;
    public static final int TYPE_FOOTER = 3;



}
