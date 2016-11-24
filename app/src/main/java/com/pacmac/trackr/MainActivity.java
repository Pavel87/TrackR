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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements NetworkStateListener {

    private TextView tLastLocation, tTimestamp, tAddress, tBatteryLevel;
    private View mapBtn;
    private ImageButton searchBtn, settingsBtn, shareBtn;
    private ImageView imageBG;
    private Dialog progressDialog = null;

    private boolean isConnected = false;
    private boolean isTrackingOn = false;
    private boolean skipConnReceiverTrigger = true;

    private AddressResultReceiver resultReceiver;
    private Handler handler;
    SharedPreferences preferences = null;
    private Firebase firebase;
    private HashMap<Integer, LocationRecord> locationRecList = new HashMap<>();
    private NetworkStateChangedReceiver connReceiver = null;

    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    private boolean isPermissionEnabled = true;


    private int recIdCount = 0;
    private ArrayList<SettingsObject> recIdDataSet = new ArrayList<SettingsObject>();
    private int itemNumber = 0;

    @Override
    protected void onCreate(Bundle savedInst) {
        super.onCreate(savedInst);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                MODE_PRIVATE);

        populateRecIdsList();
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
            //TODO save which ID is selected
//            displayDeviceLocation(false);
        } else {
            spawnReceiverIdViews();
            locationRecList = Utility.convertJsonStringToLocList(getFilesDir() + Constants.JSON_LOC_FILE_NAME);
            if( locationRecList == null){
                locationRecList =  new HashMap<>();
            }

            if (locationRecList.size() == 0) {
                displayDeviceLocation(null, true);
            } else {
                displayDeviceLocation(locationRecList.get(itemNumber), false);
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

                if (recIdDataSet.size() == 0) {
                    Utility.showToast(getApplicationContext(), getString(R.string.rec_id_wrong));
                    enableSearchButton();
                    return;
                }

                if (checkIfshouldTryRetrieveDevicePosition()) {
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
                    Utility.showToast(getApplicationContext(), getString(R.string.last_location_fresh));
                    enableSearchButton();
                    if (locationRecList.containsKey(itemNumber) && locationRecList.get(itemNumber).getAddress().equals("") && isConnected) {
                        getAdress(itemNumber);
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
                if (locationRecList.containsKey(itemNumber)) {
                    Intent sharingIntent = Utility.createShareIntent(Utility.updateShareIntent(getApplicationContext(),
                            recIdDataSet.get(itemNumber).getAlias(), locationRecList.get(itemNumber).getLatitude(),
                            locationRecList.get(itemNumber).getLongitude(), locationRecList.get(itemNumber).getTimestamp(),
                            locationRecList.get(itemNumber).getAddress(), locationRecList.get(itemNumber).getBatteryLevel()));

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
        if (Utility.checkPlayServices(getApplicationContext())) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
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
            // retrieve Location from FB for currently selected ID
            retrieveLocation(itemNumber);
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
        if (locationRecList.size() == 0) {
            Utility.showToast(getApplicationContext(), getString(R.string.no_location));
            return;
        }
        Intent intent = new Intent(this, MapDetailActivity.class);
//        intent.putExtra(Constants.KEY_LATITUDE, locationRecList.get(itemNumber).getLatitude());
//        intent.putExtra(Constants.KEY_LONGITUDE, locationRecList.get(itemNumber).getLongitude());
//        intent.putExtra(Constants.KEY_TIMESTAMP, Utility.parseDate(locationRecList.get(itemNumber).getTimestamp()));
//        intent.putExtra(Constants.KEY_ADDRESS, locationRecList.get(itemNumber).getAddress());
//        intent.putExtra(Constants.KEY_BATTERY_LEVEL, locationRecList.get(itemNumber).getBatteryLevel());
        startActivity(intent);
    }

    private void getAdress(int item) {
        if (Geocoder.isPresent() && locationRecList.containsKey(item))
            startIntentService(locationRecList.get(item), item);
        else {
            tAddress.setText(getResources().getString(R.string.not_available));
        }
    }


    private void retrieveLocation(final int item) {

        final Firebase firebase = new Firebase("https://trackr1.firebaseio.com");
        firebase.goOnline();
        Log.d(Constants.TAG, "Firebase goes online");
        firebase.keepSynced(true);
        firebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                {
                    Log.d(Constants.TAG, "Firebase goes offline");
                    firebase.removeEventListener(this);
                    firebase.goOffline();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        //Log.d(Constants.TAG, "Firebase snapshot KEY: " + snapshot.getKey());
                        for (int i = 0; i < recIdCount; i++) {
                            if (snapshot.getKey().equals(recIdDataSet.get(i).getSafeId())) {

                                // Processing received data
                                if (snapshot.hasChildren()) {
                                    Long idLong = ((Long) snapshot.child("id").getValue());
                                    int id = i;
                                    double batteryLevel = -1;
                                    if (idLong != null) {
                                        batteryLevel = (double) snapshot.child("batteryLevel").getValue();
                                    }
                                    double latitude = (double) snapshot.child("latitude").getValue();
                                    double longitude = (double) snapshot.child("longitude").getValue();
                                    long timeStamp = (long) snapshot.child("timestamp").getValue();
                                    Log.i(Constants.TAG, "Recovered data from FB for id: " + i + " alias: " + recIdDataSet.get(i).getAlias());

                                    // Store location and request addres translation
                                    locationRecList.put(i, new LocationRecord(id, latitude, longitude, timeStamp,
                                            (float) batteryLevel));


                                    // as we do multiple id request we want to display only currently selected ID
                                    if (i == itemNumber) {
                                        getAdress(i);
                                        displayDeviceLocation(locationRecList.get(i), false);
                                        Log.d(Constants.TAG, "should display this object: " + createJsonArrayString());
                                    }

                                    Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME, createJsonArrayString());
//                                    Log.d(Constants.TAG, createJsonArrayString());
                                } else if (progressDialog != null && progressDialog.isShowing()) {
                                    Utility.showToast(getApplicationContext(), getString(R.string.rec_id_wrong));
                                }
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                    // if there was network request but location is same as before then notify user
//                    if (isDataReceived && oldTimestamp == locationRecord.getTimestamp()) {
//                        // TODO may want to show how many hours/minutes ago was the location updated
//                        Utility.showToast(getApplicationContext(), getString(R.string.device_didnot_report_location));
//                    }
                                }
                                searchBtn.setEnabled(true);
                            }
                        }
                    }


//                    long timestamp = (long) snapshot.child("timestamp").getValue();
//                    String id = snapshot.getKey();
//                    if (timestamp < timeThreshold) {
//                        Log.d(Constants.TAG, id + " ID was not updated in last 7 days - likely not in use anymore");
//                        firebase.child(id).removeValue();
//                    }
                }
//
//            firebase.removeEventListener(this);
//            Log.d(Constants.TAG,"Firebase goes offline");
//            firebase.goOffline();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
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


//        String child = recIdDataSet.get(item).getSafeId();
//        Log.d(Constants.TAG, "Firebase goes online");
//        firebase.goOnline();
//        firebase.keepSynced(false);
//        firebase.child(child).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                Log.d(Constants.TAG, "Firebase goes offline");
//                firebase.removeEventListener(this);
//                firebase.goOffline();
//
//                // Processing received data
//                if (snapshot.hasChildren()) {
//                    isDataReceived = true;
//                    Long idLong = ((Long) snapshot.child("id").getValue());
//                    int id = -1;
//                    double batteryLevel = -1;
//                    if (idLong != null) {
//                        id = idLong.intValue();
//                        batteryLevel = (double) snapshot.child("batteryLevel").getValue();
//                    }
//                    double latitude = (double) snapshot.child("latitude").getValue();
//                    double longitude = (double) snapshot.child("longitude").getValue();
//                    long timeStamp = (long) snapshot.child("timestamp").getValue();
//                    Log.i(Constants.TAG, "Recovered data from FB for id: " + item + " alias: " + recIdDataSet.get(item).getAlias());
//
//                    // Store location and request addres translation
//                    locationRecList.put(item ,new LocationRecord(id, latitude, longitude, timeStamp,
//                            (float) batteryLevel));
//                    getAdress(item);
//
//
//                    // as we do multiple id request we want to display only currently selected ID
//                    if(item == itemNumber) {
//                        displayDeviceLocation(locationRecList.get(item), false);
//                    }
//                    // SAVE last known location in SharedPreferences
//                    if (preferences != null) {
//                        SharedPreferences.Editor prefEditor = preferences.edit();
//                        //TODO instead will save as JSON in file
////                        prefEditor.putInt(Constants.REMOTE_USER_ID, locationRecord.getId());
////                        prefEditor.putFloat(Constants.REMOTE_LATITUDE, (float) locationRecord.getLatitude());
////                        prefEditor.putFloat(Constants.REMOTE_LONGITUDE, (float) locationRecord.getLongitude());
////                        prefEditor.putFloat(Constants.REMOTE_BATTERY_LEVEL, (float) locationRecord.getBatteryLevel());
////                        prefEditor.putLong(Constants.REMOTE_TIMESTAMP, locationRecord.getTimestamp());
////                        prefEditor.putString(Constants.REMOTE_ADDRESS, locationRecord.getAddress());
//                        prefEditor.commit();
//                    }
//                } else if (progressDialog != null && progressDialog.isShowing()) {
//                    Utility.showToast(getApplicationContext(), getString(R.string.rec_id_wrong));
//                }
//                if (progressDialog != null && progressDialog.isShowing()) {
//                    progressDialog.dismiss();
//                    // if there was network request but location is same as before then notify user
////                    if (isDataReceived && oldTimestamp == locationRecord.getTimestamp()) {
////                        // TODO may want to show how many hours/minutes ago was the location updated
////                        Utility.showToast(getApplicationContext(), getString(R.string.device_didnot_report_location));
////                    }
//                }
//                searchBtn.setEnabled(true);
//            }
//
//            @Override
//            public void onCancelled(FirebaseError firebaseError) {
//                isDataReceived = false;
//                Log.i(Constants.TAG, "Update Cancelled" + firebaseError.getMessage());
//                firebase.goOffline();
//                Log.d(Constants.TAG, "Firebase goes offline");
//
//                // Dismiss progress dialog if it is showing right now
//                if (progressDialog != null && progressDialog.isShowing()) {
//                    progressDialog.dismiss();
//                    Utility.showToast(getApplicationContext(), getString(R.string.conn_error));
//                }
//                searchBtn.setEnabled(true);
//            }
//        });
//        }

    private void startIntentService(LocationRecord locationRecord, int item) {
        Intent intent = new Intent(getApplicationContext(), FetchAddressService.class);
        intent.putExtra(Constants.RECEIVER, resultReceiver);
        intent.putExtra(Constants.KEY_LATITUDE, locationRecord.getLatitude());
        intent.putExtra(Constants.KEY_LONGITUDE, locationRecord.getLongitude());
        intent.putExtra(Constants.KEY_ITEM_ORDER, item);
        startService(intent);
    }

    @Override
    public void connectionChanged(boolean isConnected) {
        Log.d(Constants.TAG, "Conn changed: " + isConnected);
        this.isConnected = isConnected;
        if (isConnected && !skipConnReceiverTrigger) {
            checkIfshouldTryRetrieveDevicePosition();
        }
        skipConnReceiverTrigger = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        skipConnReceiverTrigger = true;
        registerReceiver(connReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        if (!refreshViewsIfChangeOccured()) {
            checkIfshouldTryRetrieveDevicePosition();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connReceiver);
        startTrackingService();
        // save loc collection before exit
        Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME, createJsonArrayString());
    }

    private boolean checkIfshouldTryRetrieveDevicePosition() {
        if (recIdDataSet.size() == 0) {
            return false;
        }
        boolean shouldConnectToFB = true;


//
//        // If I have location record and timestamp of last device upload is smalled than 15 minutes then I don't want to do update

        for (int i = 0; i < recIdCount; i++) {

            if (locationRecList.containsKey(i)) {
                shouldConnectToFB = !(locationRecList.get(i).getTimestamp() > (System.currentTimeMillis() - Constants.UPDATE_TIMEOUT));
            }
            if (shouldConnectToFB) {
                break;
            }
        }

        // TODO ids have been refreshed then we want to update
//        // but if Ids changes since last time then we want to try to fetch new data
//        if (!recIdFromPref.equals(receivingId)) {
//            shouldConnectToFB = true;
//            receivingId = recIdFromPref;
//            resetLocationAndRefreshScreen();
//        }

        if (shouldConnectToFB) {
            getLastKnownLocation();
        }
        return shouldConnectToFB;
    }

    private void resetLocationAndRefreshScreen() {
        if (preferences != null) {
            int id = preferences.getInt(Constants.REMOTE_USER_ID, -1);
            if (id == -1) {
//                locationRecord = null;
            }
        }
        // Will clear the old device location
        displayDeviceLocation(null, true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (locationRecList.size() != 0) {
            outState.putInt(Constants.KEY_ITEM_ORDER, itemNumber);

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

            String address = "";
            int itemOrder = resultData.getInt(Constants.KEY_ITEM_ORDER, 0);

            if (locationRecList.containsKey(itemOrder)) {
                if (resultCode == Constants.SUCCESS) {
                    address = resultData.getString(Constants.RESULT_DATA_KEY);
                } else { // GEOCODER returned error
                    address = getResources().getString(R.string.not_available);
                    Log.e(Constants.TAG, address);
                }
                if (itemOrder == itemNumber) {
                    tAddress.setText(address);
                }

                // Store address in proper location object
                locationRecList.get(itemOrder).setAddress(address);
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

    private void displayDeviceLocation(LocationRecord locationRecord, boolean isLoading) {

        if (!isLoading) {
            tLastLocation.setText(locationRecord.toString());
            tTimestamp.setText(Utility.parseDate(locationRecord.getTimestamp()));
            if (locationRecord.getBatteryLevel() < 20) {
                tBatteryLevel.setTextColor(getResources().getColor(R.color.text_critical));
            } else {
                tBatteryLevel.setTextColor(getResources().getColor(R.color.text_nightMode));
            }
            tBatteryLevel.setText(String.format("%.0f", locationRecord.getBatteryLevel()) + " %");
            if (!locationRecord.getAddress().equals("")) {
                tAddress.setText(locationRecord.getAddress());
            } else {
                getAdress(itemNumber);
                tAddress.setText("");
            }

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
                // TODO check this message
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


    private void populateRecIdsList() {
        String jsonString = Utility.loadJsonStringFromFile(getFilesDir() + Constants.JSON_REC_IDS_FILE_NAME);

        if (jsonString.equals("")) {
            // file doesn't exist
            return;
        }

        try {
            JSONObject jsnobject = new JSONObject(jsonString);
            JSONArray jsonArray = jsnobject.getJSONArray("receiverids");
            recIdDataSet.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                SettingsObject settingsObject = Utility.createSettingsObjectFromJson((JSONObject) jsonArray.get(i));
                if (settingsObject != null) {
                    recIdDataSet.add(settingsObject);
                }
            }
        } catch (JSONException e) {
            Log.d(Constants.TAG, "#4# Error getting JSON obj or array. " + e.getMessage());
        }
        recIdCount = recIdDataSet.size();
    }

    private void spawnReceiverIdViews() {
        final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.receivingIDPanel);
        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < recIdCount; i++) {
            View view = inflater.inflate(R.layout.tracking_id_button_layout, null);
            TextView item = (TextView) view.findViewById(R.id.recIdLabel);
            if (i == 0) item.setSelected(true);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (!view.isSelected()) {
                        view.setAnimation(Utility.getAnimation());

                        itemNumber = Integer.parseInt(((TextView) view).getHint().toString());
                        invalidateRecButtonsViews(linearLayout);
                        view.setSelected(true);
                        // check if we already hav any location for this id first
                        if (locationRecList.containsKey(itemNumber)) {
                            displayDeviceLocation(locationRecList.get(itemNumber), false);
                        } else {
                            getLastKnownLocation();
                            displayDeviceLocation(null, true);
                        }
                    }
                }
            });
            item.setText(recIdDataSet.get(i).getAlias().substring(0, 1));
            item.setHint(String.valueOf(i));
            linearLayout.addView(view);

            if (recIdDataSet.size() > 0) {
                itemNumber = 0;
            }
        }
    }


    private void invalidateRecButtonsViews(LinearLayout linearLayout) {
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            linearLayout.getChildAt(i).findViewById(R.id.recIdLabel).setSelected(false);
        }
    }

    private String createJsonArrayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{\"locrecords\":[");

        int counter = 0;
        for (Map.Entry<Integer, LocationRecord> entry : locationRecList.entrySet()) {
            if (counter == (locationRecList.size() - 1)) {
                sb.append(entry.getValue().getJSONString());
                break;
            }
            sb.append(entry.getValue().getJSONString() + ",");
            counter++;
        }
        sb.append("]}");
        return sb.toString();
    }




    private boolean refreshViewsIfChangeOccured() {

        if (preferences == null) {
            preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                    MODE_PRIVATE);
        }

        if (preferences.getBoolean(Constants.RECEIVING_ID_CHANGE, false)) {

            // TODO maybe we can wipe only changed ids and its location

            recIdDataSet.clear();
            locationRecList.clear();

            LinearLayout view = (LinearLayout) findViewById(R.id.receivingIDPanel);
            view.removeAllViews();

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.RECEIVING_ID_CHANGE, false);
            editor.commit();
            // display loading screen
            displayDeviceLocation(null, true);

            populateRecIdsList();
            spawnReceiverIdViews();
            getLastKnownLocation();
            return true;
        }
        return false;
    }


}

