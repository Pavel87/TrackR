package com.pacmac.trackr;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class FetchFirebaseData extends Service {

    private final static String TAG = "FetchFirebaseData";
    private List<LocationRecord> userRecords = new ArrayList<>();
    private boolean isAddressResolverRegistred = false;
    private long lastFirebaseFetchTimestamp = 0L;

    private static final long ONE_HOUR = 60 * 60L;
    private static final long FOUR_HOUR = 4 * 60 * 60L;
    private static final long EIGHT_HOUR = 8 * 60 * 60L;
    private static final long ONE_DAY = 24 * 60 * 60L;
    private static final long THREE_DAYS = 3 * 2460 * 60L;

    private static final long DELAY_20_MIN = 30 * 60 * 1000L;
    private static final long DELAY_60_MIN = 60 * 60 * 1000L;
    private static final long DELAY_6_HOURS = 6 * 60 * 60 * 1000L;
    private static final long DELAY_12_HOURS = 12 * 60 * 60 * 1000L;
    private static final long DELAY_36_HOURS = 36 * 60 * 60 * 1000L;

    private Handler handler = new Handler();

    private Runnable retrieveUsersRunnable = new Runnable() {
        @Override
        public void run() {
            if (Utility.checkConnectivity(getApplicationContext())) {
                handler.postDelayed(this, 10 * 60 * 1000L);
                return;
            }
            handler.postDelayed(this, 20 * 60 * 1000L);
            retrieveLocation();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        userRecords = Utility.convertJsonStringToUserRecords(getFilesDir() + Constants.JSON_LOC_FILE_NAME);
        Log.w(TAG, "onStartCommand");
        Log.d(TAG, "userRecords.size: " + userRecords.size());
        if (userRecords.size() > 0 && !userRecords.get(0).getRecId().equals("")) {
            handler.postDelayed(retrieveUsersRunnable, 20 * 60 * 1000L);
            return START_STICKY;
        } else {
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterAddressResolverReceiver();
        if (handler != null) {
            handler.removeCallbacks(retrieveUsersRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void retrieveLocation() {

        SharedPreferences preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                MODE_PRIVATE);
        long lastUserRunTS = preferences.getLong(Constants.LAST_APP_OPEN_TS, 0L);
        if (lastUserRunTS == 0L) {
            return;
        }

        if (!isTimeToFetchFirebaseData(lastUserRunTS)) {
            return;
        }

        userRecords = Utility.convertJsonStringToUserRecords(getFilesDir() + Constants.JSON_LOC_FILE_NAME);
        if(userRecords.size() == 0) {
            stopSelf();
            return;
        }

        lastFirebaseFetchTimestamp = System.currentTimeMillis();
        registerAddressResolverReceiver();

        /** FETCH FIRESTORM / FIREBASE DATA **/
        FirebaseHandler.fetchFirebaseData(getApplicationContext(), userRecords, null);
    }

    private void registerAddressResolverReceiver() {
        if (!isAddressResolverRegistred) {
            IntentFilter intentFilter = new IntentFilter(Constants.ADDRESS_RESOLVER_ACTION);
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(addressResolverReceiver, intentFilter);
            isAddressResolverRegistred = true;
        }
    }

    private void unRegisterAddressResolverReceiver() {
        if (isAddressResolverRegistred) {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(addressResolverReceiver);
            isAddressResolverRegistred = false;
        }
    }

    /**
     * Adress resolver Receiver
     */
    private BroadcastReceiver addressResolverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String address = intent.getStringExtra(Constants.ADDRESS_RESOLVER_ADDRESS);
            int rowId = intent.getIntExtra(Constants.ADDRESS_RESOLVER_ROWID, -1);
            // make sure to return if rowId is out of bounds for userRecords
            if (rowId == -1 || userRecords.size() <= rowId) {
                return;
            }
            //update userRecords with new address and invalidate row
            userRecords.get(rowId).setAddress(address);
            // store address in local file
            Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                    Utility.createJsonArrayStringFromUserRecords(userRecords));
        }
    };

    protected boolean isTimeToFetchFirebaseData(long lastUserInitAppStart) {
        long currentTime = System.currentTimeMillis();
        long diff = (currentTime - lastUserInitAppStart) / 1000;
        if (diff <= ONE_HOUR) {
            if (currentTime > lastFirebaseFetchTimestamp + DELAY_20_MIN) {
                return true;
            }
        } else if (diff <= FOUR_HOUR) {
            if (currentTime > lastFirebaseFetchTimestamp + DELAY_60_MIN) {
                return true;
            }
        } else if (diff <= EIGHT_HOUR) {
            if (currentTime > lastFirebaseFetchTimestamp + DELAY_6_HOURS) {
                return true;
            }
        } else if (diff <= ONE_DAY) {
            if (currentTime > lastFirebaseFetchTimestamp + DELAY_12_HOURS) {
                return true;
            }
        } else if (diff <= THREE_DAYS) {
            if (currentTime > lastFirebaseFetchTimestamp + DELAY_36_HOURS) {
                return true;
            }
        } else {
            stopSelf();
            return false;
        }
        return false;
    }
}
