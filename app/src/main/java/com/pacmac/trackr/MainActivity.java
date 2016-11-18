package com.pacmac.trackr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class MainActivity extends AppCompatActivity implements NetworkStateListener {

    private TextView tLastLocation, tTimestamp, tAddress, tBatteryLevel;
    private View mapBtn;
    private ImageButton searchBtn, settingsBtn, shareBtn;
    private ImageView imageBG;
    private Dialog progressDialog = null;

    private boolean isConnected = false;
    private boolean isTrackingOn = false;
    private boolean isDataReceived = false;

    private AddressResultReceiver resultReceiver;
    private Handler handler;
    SharedPreferences preferences = null;
    private Firebase firebase;
    private LocationRecord locationRecord;
    private NetworkStateChangedReceiver connReceiver = null;

    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    private boolean isPermissionEnabled = true;

    private String receivingId = "";

    @Override
    protected void onCreate(Bundle savedInst) {
        super.onCreate(savedInst);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                MODE_PRIVATE);

        // Check if user disabled LOCATION permission at some point
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            isPermissionEnabled = Utility.checkPermission(getApplicationContext(),
                    LOCATION_PERMISSION);
        }
        if (!isPermissionEnabled) {
            Utility.displayExplanationForPermission(this, LOCATION_PERMISSION);
        }

        tLastLocation = (TextView) findViewById(R.id.coordinates);
        tTimestamp = (TextView) findViewById(R.id.timestamp);
        tAddress = (TextView) findViewById(R.id.address);
        tBatteryLevel = (TextView) findViewById(R.id.batteryLevel);

        mapBtn = findViewById(R.id.showMap);
        searchBtn = (ImageButton) findViewById(R.id.search);
        settingsBtn = (ImageButton) findViewById(R.id.settings);
        shareBtn = (ImageButton) findViewById(R.id.share);

        imageBG = (ImageView) findViewById(R.id.imgBG);
        // renderScript Class works only from API 17 so have to check if it can
        // blur the bg or not
        if (Build.VERSION.SDK_INT > 16) {
            imageBG.setImageBitmap(setBitmap());
        }

        handler = new Handler();
        resultReceiver = new AddressResultReceiver(handler);
        connReceiver = new NetworkStateChangedReceiver();
        connReceiver.setConnectionListener(this);

        // restore location on reconfiguration
        if (savedInst != null && savedInst.getInt(Constants.KEY_ID, -1) != -1) {
            locationRecord = new LocationRecord(savedInst.getInt(Constants.KEY_ID, -1),
                    savedInst.getDouble(Constants.KEY_LATITUDE),
                    savedInst.getDouble(Constants.KEY_LONGITUDE),
                    savedInst.getLong(Constants.KEY_TIMESTAMP),
                    savedInst.getDouble(Constants.KEY_BATTERY_LEVEL, 100),
                    savedInst.getString(Constants.KEY_ADDRESS));
            receivingId = savedInst.getString(Constants.RECEIVING_ID, "");
            displayDeviceLocation(false);
        } else {
            locationRecord = restoreLocationRecordFromPref();
            if (locationRecord != null) {
                displayDeviceLocation(false);
            }
        }

        checkConnectivity();

        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMap();
            }
        });
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBtn.setEnabled(false);

                if (!isConnected) {
                    Utility.showToast(getApplicationContext(), getString(R.string.no_connection));
                    enableSearchButton();
                    return;
                }

                if (preferences.getString(Constants.RECEIVING_ID, "").length() < 8) {
                    Utility.showToast(getApplicationContext(), getString(R.string.rec_id_wrong));
                    enableSearchButton();
                    return;
                }

                if (checkIfshouldTryRetrieveDevicePosition()) {
                    isDataReceived = false;
                    if (progressDialog == null) {
                        progressDialog = new Dialog(MainActivity.this);
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        }
                        LayoutInflater inflater = getLayoutInflater();
                        View view = inflater.inflate(R.layout.trackr_progress_dialog, null);
                        TextView textView = (TextView) view.findViewById(R.id.progressText);
                        textView.setText(getString(R.string.progress_searching));
                        progressDialog.setContentView(view);
                        progressDialog.setCancelable(false);
                        progressDialog.show();
                    }
                    progressDialog.show();
                    new Handler().postDelayed(dismissDialogRunnable, 20 * 1000);
                } else {
                    isDataReceived = false;
                    Utility.showToast(getApplicationContext(), getString(R.string.last_location_fresh));
                    enableSearchButton();
                    if (locationRecord != null && locationRecord.getAddress().equals("") && isConnected) {
                        getAdress();
                    }
                }
            }
        });
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationRecord != null) {
                    Intent sharingIntent = Utility.createShareIntent(Utility.updateShareIntent(getApplicationContext(), locationRecord.getLatitude(),
                            locationRecord.getLongitude(), locationRecord.getTimestamp(),
                            locationRecord.getAddress(), locationRecord.getBatteryLevel()));

                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.extract_data)));
                } else {
                    Utility.showToast(getApplicationContext(), getString(R.string.no_location));
                }
            }
        });
        Firebase.setAndroidContext(getApplicationContext());
        if (firebase == null) {
            firebase = new Firebase("https://trackr1.firebaseio.com");
        }
        Log.d(Constants.TAG, "Firebase goes offline");
        firebase.goOffline();
        // at last we want to start tracking service if not started and if
        // device is in tracking mode
        startTrackingService();

        showUpdateDialog();
        showRateMyAppDialog();
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void showUpdateDialog() {
        String appVersion = Utility.getCurrentAppVersion(getApplicationContext());

        if (!preferences.getString(Constants.NEW_UPDATE, "").equals(appVersion)) {
            Utility.createAlertDialog(MainActivity.this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.NEW_UPDATE, appVersion);
            editor.commit();
        }
    }

    private void showRateMyAppDialog() {
        int counter = preferences.getInt(Constants.RATING_POPUP_COUNTER, 0);
        counter++;
        if (counter > Constants.RATING_POPUP_ATTEMPTS) {
            counter = 0;
            Utility.showRateMyAppDialog(MainActivity.this, preferences);
        }
        preferences.edit().putInt(Constants.RATING_POPUP_COUNTER, counter).commit();
    }

    private Bitmap setBitmap() {

        BitmapDrawable drawable = (BitmapDrawable) imageBG.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        Bitmap blurred = Blurring.getBitmapBlurry(bitmap, 10, getApplicationContext());
        return blurred;
    }

    private void checkConnectivity() {
        ConnectivityManager conn = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conn.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            isConnected = true;
        } else {
            isConnected = false;
        }
    }

    private void getLastKnownLocation() {
        if (isConnected) {
            retrieveLocation();
        } else {
            Utility.showToast(getApplicationContext(), getString(R.string.no_connection));
            enableSearchButton();
        }
    }

    private void openMap() {
        if (!isConnected) {
            Utility.showToast(getApplicationContext(), getString(R.string.no_connection));
            return;
        }
        if (locationRecord == null) {
            Utility.showToast(getApplicationContext(), getString(R.string.no_location));
            return;
        }
        Intent intent = new Intent(this, MapDetailActivity.class);
        intent.putExtra(Constants.KEY_LATITUDE, locationRecord.getLatitude());
        intent.putExtra(Constants.KEY_LONGITUDE, locationRecord.getLongitude());
        intent.putExtra(Constants.KEY_TIMESTAMP, Utility.parseDate(locationRecord.getTimestamp()));
        intent.putExtra(Constants.KEY_ADDRESS, locationRecord.getAddress());
        intent.putExtra(Constants.KEY_BATTERY_LEVEL, locationRecord.getBatteryLevel());
        startActivity(intent);
    }

    private void getAdress() {
        if (Geocoder.isPresent())
            startIntentService();
        else {
            tAddress.setText(getResources().getString(R.string.not_available));
        }
    }

    private void retrieveLocation() {
        String child = preferences.getString(Constants.RECEIVING_ID, "child_none");
        Log.d(Constants.TAG, "Firebase goes online");
        firebase.goOnline();
        firebase.keepSynced(false);
        firebase.child(child).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(Constants.TAG, "Firebase goes offline");
                firebase.goOffline();
                long oldTimestamp = 0l;
                if (locationRecord != null) {
                    oldTimestamp = locationRecord.getTimestamp();
                }
                if (snapshot.hasChildren()) {
                    isDataReceived = true;
                    Long idLong = ((Long) snapshot.child("id").getValue());
                    int id = -1;
                    double batteryLevel = -1;
                    if (idLong != null) {
                        id = idLong.intValue();
                        batteryLevel = (double) snapshot.child("batteryLevel").getValue();
                    }
                    double latitude = (double) snapshot.child("latitude").getValue();
                    double longitude = (double) snapshot.child("longitude").getValue();
                    long timeStamp = (long) snapshot.child("timestamp").getValue();
                    Log.i(Constants.TAG, "recovered batt level from FB: " + batteryLevel);

                    locationRecord = new LocationRecord(id, latitude, longitude, timeStamp,
                            (float) batteryLevel);
                    getAdress();
                    firebase.removeEventListener(this);

                    displayDeviceLocation(false);

                    // SAVE last known location in SharedPreferences
                    if (preferences != null) {
                        SharedPreferences.Editor prefEditor = preferences.edit();
                        prefEditor.putInt(Constants.REMOTE_USER_ID, locationRecord.getId());
                        prefEditor.putFloat(Constants.REMOTE_LATITUDE, (float) locationRecord.getLatitude());
                        prefEditor.putFloat(Constants.REMOTE_LONGITUDE, (float) locationRecord.getLongitude());
                        prefEditor.putFloat(Constants.REMOTE_BATTERY_LEVEL, (float) locationRecord.getBatteryLevel());
                        prefEditor.putLong(Constants.REMOTE_TIMESTAMP, locationRecord.getTimestamp());
                        prefEditor.putString(Constants.REMOTE_ADDRESS, locationRecord.getAddress());
                        prefEditor.commit();
                    }
                } else if (progressDialog != null && progressDialog.isShowing()) {
                    Utility.showToast(getApplicationContext(), getString(R.string.rec_id_wrong));
                }
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    // if there was network request but location is same as before then notify user
                    if (isDataReceived && oldTimestamp == locationRecord.getTimestamp()){
                        // TODO may want to show how many hours/minutes ago was the location updated
                        Utility.showToast(getApplicationContext(), getString(R.string.device_didnot_report_location));
                    }
                }
                searchBtn.setEnabled(true);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                isDataReceived = false;
                Log.i(Constants.TAG, "Update Cancelled" + firebaseError.getMessage());
                firebase.goOffline();
                Log.d(Constants.TAG, "Firebase goes offline");

                // Dismiss progress dialog if it is showing right now
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Utility.showToast(getApplicationContext(), getString(R.string.conn_error));
                }
                searchBtn.setEnabled(true);
            }
        });
    }

    private void startIntentService() {
        Intent intent = new Intent(getApplicationContext(), FetchAddressService.class);
        intent.putExtra(Constants.RECEIVER, resultReceiver);
        intent.putExtra(Constants.KEY_LATITUDE, locationRecord.getLatitude());
        intent.putExtra(Constants.KEY_LONGITUDE, locationRecord.getLongitude());
        startService(intent);
    }

    @Override
    public void connectionChanged(boolean isConnected) {
        Log.d(Constants.TAG, "Conn changed: " + isConnected);
        this.isConnected = isConnected;
        if (isConnected)
            checkIfshouldTryRetrieveDevicePosition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(connReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        checkIfshouldTryRetrieveDevicePosition();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connReceiver);
        startTrackingService();
    }

    private boolean checkIfshouldTryRetrieveDevicePosition() {
        String recIdFromPref = preferences.getString(Constants.RECEIVING_ID, null);
        boolean shouldConnectToFB = true;

        if (recIdFromPref == null || recIdFromPref.equals("")) {
            return false;
        }

        // If I have location record and timestamp of last device upload is smalled than 15 minutes then I don't want to do update
        if (locationRecord != null && locationRecord.getTimestamp() > (System.currentTimeMillis() - Constants.UPDATE_TIMEOUT)) {
            shouldConnectToFB = false;
        }

        // but if Ids changes since last time then we want to try to fetch new data
        if (!recIdFromPref.equals(receivingId)) {
            shouldConnectToFB = true;
            receivingId = recIdFromPref;
            resetLocationAndRefreshScreen();
        }

        if (shouldConnectToFB) {
            getLastKnownLocation();
        }
        return shouldConnectToFB;
    }

    private void resetLocationAndRefreshScreen() {
        if (preferences != null) {
            int id = preferences.getInt(Constants.REMOTE_USER_ID, -1);
            if (id == -1) {
                locationRecord = null;
            }
        }
        // Will clear the old device location
        displayDeviceLocation(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (locationRecord != null) {
            outState.putString(Constants.RECEIVING_ID, receivingId);
            outState.putInt(Constants.KEY_ID, locationRecord.getId());
            outState.putDouble(Constants.KEY_LATITUDE, locationRecord.getLatitude());
            outState.putDouble(Constants.KEY_LONGITUDE, locationRecord.getLongitude());
            outState.putLong(Constants.KEY_TIMESTAMP, locationRecord.getTimestamp());
            outState.putString(Constants.KEY_ADDRESS, locationRecord.getAddress());
            outState.putDouble(Constants.KEY_BATTERY_LEVEL, locationRecord.getBatteryLevel());
        }
        super.onSaveInstanceState(outState);
    }


    /// CLASS TO RESOLVE ADDRESS
    @SuppressLint("ParcelCreator")
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            String address = resultData.getString(Constants.RESULT_DATA_KEY);
            if (resultCode == Constants.SUCCESS) {
                locationRecord.setAddress(address);
                tAddress.setText(address);
            } else { // GEOCODER returned error
                locationRecord.setAddress(getResources().getString(R.string.not_available));
                tAddress.setText(getResources().getString(R.string.not_available));
                Log.e(Constants.TAG, address);
            }
            // save address to shared preferences
            if (preferences != null) {
                SharedPreferences.Editor prefEditor = preferences.edit();
                prefEditor.putString(Constants.REMOTE_ADDRESS, locationRecord.getAddress());
                prefEditor.commit();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (requestCode == Utility.MY_PERMISSIONS_REQUEST) {
            isPermissionEnabled = Utility.checkPermission(getApplicationContext(),
                    LOCATION_PERMISSION);
        }
        if (isPermissionEnabled) {
            checkIfshouldTryRetrieveDevicePosition();
        }
    }

    private void startTrackingService() {
        isTrackingOn = preferences.getBoolean(Constants.TRACKING_STATE, false);
        if (isTrackingOn && !isMyServiceRunning(LocationService.class)) {
            Intent intentService = new Intent(getApplicationContext(), LocationService.class);
            startService(intentService);
        }
    }

    /**
     * check the given service is running
     *
     * @param serviceClass class eg MyService.class
     * @return boolean
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    // this will restore last known location from shared preferences on start if available
    private LocationRecord restoreLocationRecordFromPref() {
        if (preferences != null) {
           receivingId = preferences.getString(Constants.RECEIVING_ID, "");

            LocationRecord locRecord = new LocationRecord();

            int id = preferences.getInt(Constants.REMOTE_USER_ID, -1);
            if (id == -1) {
                return null;
            }
            locRecord.setId(id);
            locRecord.setLatitude((double) preferences.getFloat(Constants.REMOTE_LATITUDE, 0));
            locRecord.setLongitude((double) preferences.getFloat(Constants.REMOTE_LONGITUDE, 0));
            locRecord.setBatteryLevel((double) preferences.getFloat(Constants.REMOTE_BATTERY_LEVEL, -1));
            locRecord.setTimestamp(preferences.getLong(Constants.REMOTE_TIMESTAMP, 0));
            locRecord.setAddress(preferences.getString(Constants.REMOTE_ADDRESS, getString(R.string.not_available)));
            return locRecord;
        }
        return null;
    }

    private void displayDeviceLocation(boolean isLoading) {
        if (!isLoading) {
            tLastLocation.setText(locationRecord.toString());
            tTimestamp.setText(Utility.parseDate(locationRecord.getTimestamp()));
            if (locationRecord.getBatteryLevel() < 20) {
                tBatteryLevel.setTextColor(getResources().getColor(R.color.text_critical));
            } else {
                tBatteryLevel.setTextColor(getResources().getColor(R.color.text_nightMode));
            }
            tBatteryLevel.setText(String.format("%.0f", locationRecord.getBatteryLevel()) + " %");
            if (!locationRecord.getAddress().equals(""))
                tAddress.setText(locationRecord.getAddress());

//            if (progressDialog != null && progressDialog.isShowing()) {
//                progressDialog.dismiss();
//            }
        } else {
            tLastLocation.setText(getString(R.string.loading));
            tTimestamp.setText("");
            tAddress.setText("");
            tBatteryLevel.setText("");
        }

    }


    private Runnable dismissDialogRunnable = new Runnable() {
        @Override
        public void run() {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                Utility.showToast(getApplicationContext(), getString(R.string.last_location_fresh));
            }
            searchBtn.setEnabled(true);
        }
    };

    private void enableSearchButton() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                searchBtn.setEnabled(true);
            }
        }, 4000);
    }
}