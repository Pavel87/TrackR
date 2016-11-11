package com.pacmac.trackr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
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
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements NetworkStateListener {

    private TextView tLastLocation, tTimestamp, tAddress, tBatteryLevel;
    private View mapBtn;
    private ImageButton searchBtn, settingsBtn, shareBtn;
    private ImageView imageBG;

    private boolean haveLocation = false;
    private boolean isConnected = false;
    private boolean isTrackingOn = false;

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

        mapBtn = (View) findViewById(R.id.showMap);
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
            tLastLocation.setText(locationRecord.toString());
            tTimestamp.setText(locationRecord.getFormatedTimestamp());
            tAddress.setText(locationRecord.getAddress());

            double battery = locationRecord.getBatteryLevel();

            if (battery < 20) {
                tBatteryLevel.setTextColor(getResources().getColor(R.color.text_critical));
            } else {
                tBatteryLevel.setTextColor(getResources().getColor(R.color.text_nightMode));
            }
            tBatteryLevel.setText(String.format("%.2f", battery) + " %");
        }

        // generate unique IDs on first run
        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                MODE_PRIVATE);
        if (preferences.getBoolean(Constants.FIRST_RUN, true)) {
            SharedPreferences.Editor editor = preferences.edit();
            String uniqueID = generateUniqueID().substring(0, 24);
            editor.putString(Constants.TRACKING_ID, uniqueID);
            editor.putString(Constants.TRACKING_ID_RAW, uniqueID);
            editor.putString(Constants.RECEIVING_ID_RAW, uniqueID);
            editor.putString(Constants.RECEIVING_ID, uniqueID);
            editor.putBoolean(Constants.FIRST_RUN, false);
            editor.commit();
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
                getLastKnownLocation();
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

                    startActivity(Intent.createChooser(sharingIntent, "Extract Device Location To:"));
                }
            }
        });

        Firebase.setAndroidContext(getApplicationContext());
        firebase = new Firebase("https://trackr1.firebaseio.com");
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
        } else
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
    }

    private void openMap() {
        if (!isConnected) {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
            return;
        }
        if (locationRecord == null) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_location),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, MapDetailActivity.class);
        intent.putExtra(Constants.KEY_LATITUDE, locationRecord.getLatitude());
        intent.putExtra(Constants.KEY_LONGITUDE, locationRecord.getLongitude());
        intent.putExtra(Constants.KEY_TIMESTAMP, locationRecord.getFormatedTimestamp());
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
        String child = preferences.getString(Constants.RECEIVING_ID, "Error");
        firebase.keepSynced(true);
        firebase.child(child).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChildren()) {

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
                    Log.d(Constants.TAG, "recovered batt level from FB: " + batteryLevel);

                    locationRecord = new LocationRecord(id, latitude, longitude, timeStamp,
                            (float) batteryLevel);
                    haveLocation = true;

                    String date = Utility.parseDate(locationRecord.getTimestamp());
                    tLastLocation.setText(locationRecord.toString());
                    tTimestamp.setText(date);
                    getAdress();

                    if (batteryLevel < 20) {
                        tBatteryLevel.setTextColor(getResources().getColor(R.color.text_critical));
                    } else {
                        tBatteryLevel.setTextColor(getResources().getColor(R.color.text_nightMode));
                    }
                    tBatteryLevel.setText(String.format("%.0f", batteryLevel) + " %");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.i(Constants.TAG, "Update Cancelled" + firebaseError.getMessage());
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
        haveLocation = false;
        unregisterReceiver(connReceiver);
        startTrackingService();
    }

    private void checkIfshouldTryRetrieveDevicePosition() {
        String recIdFromPref = preferences.getString(Constants.RECEIVING_ID, null);
        if (recIdFromPref == null) {
            return;
        }
        if (locationRecord != null && locationRecord.getTimestamp() > (System.currentTimeMillis() - Constants.TIME_BATTERY_OK) && recIdFromPref.equals(receivingId)) {
            return;
        }
        receivingId = recIdFromPref;
        getLastKnownLocation();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        if (locationRecord != null) {
            outState.putInt(Constants.KEY_ID, locationRecord.getId());
            outState.putDouble(Constants.KEY_LATITUDE, locationRecord.getLatitude());
            outState.putDouble(Constants.KEY_LONGITUDE, locationRecord.getLongitude());
            outState.putLong(Constants.KEY_TIMESTAMP, locationRecord.getTimestamp());
            outState.putString(Constants.KEY_ADDRESS, locationRecord.getAddress());
            outState.putDouble(Constants.KEY_BATTERY_LEVEL, locationRecord.getBatteryLevel());
        }
        super.onSaveInstanceState(outState);
    }

    private String generateUniqueID() {
        return UUID.randomUUID().toString();
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
            getLastKnownLocation();
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

}
