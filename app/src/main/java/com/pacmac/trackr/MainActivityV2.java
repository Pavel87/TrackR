package com.pacmac.trackr;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pacmac.trackr.mapmarker.IconGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by pacmac on 2017-08-05.
 */


public class MainActivityV2 extends FragmentActivity implements OnMapReadyCallback,
        NetworkStateListener, TrackListMainAdapter.TrackListItemSelectedListener {

    private static final String TAG = "TrackRMain";

    SharedPreferences preferences = null;

    private GoogleMap mMap;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    ImageButton settingsBtn;

    private NetworkStateChangedReceiver connReceiver = null;
    private boolean isConnected = false;
    private boolean skipConnReceiverTrigger = true;

    private Firebase firebase;
    private List<LocationRecord> userRecords = new ArrayList<>();
    private int currentTracker = 0;

    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    private boolean isPermissionEnabled = true;

    private boolean skipfbCallOnReconfiguration = false;


    private boolean isAddressResolverRegistred = false;
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
            mAdapter.notifyItemChanged(rowId);
            // store address in local file
            Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                    Utility.createJsonArrayStringFromUserRecords(userRecords));
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_v2);
        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                MODE_PRIVATE);

        Firebase.setAndroidContext(getApplicationContext());
        populateUserRecords();

        // restore location on reconfiguration
        if (savedInstanceState != null) {
            currentTracker = savedInstanceState.getInt(Constants.KEY_ITEM_ORDER, 0);
            skipfbCallOnReconfiguration = true;

        }

        connReceiver = new NetworkStateChangedReceiver();
        connReceiver.setConnectionListener(this);

        mRecyclerView = findViewById(R.id.trackList);
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new TrackListMainAdapter(userRecords, getApplicationContext());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setVerticalScrollBarEnabled(false);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                Log.e(TAG, " DX:" + dx + " DY:" + dy);
            }
        });

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            isPermissionEnabled = Utility.checkPermission(getApplicationContext(),
                    LOCATION_PERMISSION);
        }
        if (!isPermissionEnabled) {
            Utility.displayExplanationForPermission(this, LOCATION_PERMISSION);
        }

        checkConnectivity();
        Utility.startTrackingService(getApplicationContext(), preferences);
        showUpdateDialog();
        showRateMyAppDialog();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();

//        FragmentManager fragmentManager = getFragmentManager();
//        fragmentManager.beginTransaction().add(R.id.map_container, new MapFragment(), "TrackRMapFragment").commit();

        getSupportFragmentManager().beginTransaction().add(R.id.map_container, mapFragment).commit();
        mapFragment.getMapAsync(this);

        // Controls
        settingsBtn = findViewById(R.id.settings);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.openSettings(getApplicationContext(), MainActivityV2.this);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        skipConnReceiverTrigger = true;
        registerReceiver(connReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        if (!refreshViewsIfChangeOccured(false)) {
            showUsersLocationOnMap();
            checkIfshouldTryRetrieveDevicePosition();
            if (userRecords == null || userRecords.size() == 0) {
                //Utility.showToast(getApplicationContext(), getString(R.string.rec_id_wrong));
                // tLastLocation.setText(getString(R.string.no_rec_id_found));
                //  enableSearchButton();
                return;
            }
            // tryToRetrieveNewLocationWithProgress(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connReceiver);
        unRegisterAddressResolverReceiver();
        Utility.startTrackingService(getApplicationContext(), preferences);
        // save loc collection before exit
        Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                Utility.createJsonArrayStringFromUserRecords(userRecords));
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {

        super.onSaveInstanceState(outState, outPersistentState);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                CircleOptions cOptions = new CircleOptions();
                cOptions.center(marker.getPosition()).fillColor(getResources().getColor(R.color.marker_area))
                        .strokeColor(getResources().getColor(R.color.map_radius)).radius(15).strokeWidth(0.6f).visible(true);
                mMap.addCircle(cOptions);
                return false;
            }
        });

        showUsersLocationOnMap();

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (userRecords != null && userRecords.size() != 0) {
            outState.putInt(Constants.KEY_ITEM_ORDER, currentTracker);
        }
        super.onSaveInstanceState(outState);
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
    public void OnItemSelected(int position) {
        currentTracker = position;
        if(mMap != null) {
            if (userRecords.get(currentTracker).getLatitude() != 0 || userRecords.get(currentTracker).getLongitude() != 0) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userRecords.get(currentTracker).getLatitude(),
                        userRecords.get(currentTracker).getLongitude()), 16f));
            } else {
                Utility.showToast(getApplicationContext(), "Ups nothing to show for " + userRecords.get(currentTracker).getAlias());
            }
        }
    }

    /**
     * Methods
     */


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

       private void populateUserRecords() {
        userRecords = Utility.convertJsonStringToUserRecords(getFilesDir() + Constants.JSON_LOC_FILE_NAME);
        String recIdsJsonString = Utility.loadJsonStringFromFile(getFilesDir() + Constants.JSON_REC_IDS_FILE_NAME);

        if (recIdsJsonString.equals("")) {
            // file doesn't exist
            //backward compatibility
            String recId = preferences.getString(Constants.RECEIVING_ID, "");
            String safeId = preferences.getString(Constants.RECEIVING_ID_RAW, "");

            // TODO this piece might be removed later as it is only for pre v2 upgrades
            if (!recId.equals("")) {
                // we likely upgraded from older version delete pref here
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(Constants.RECEIVING_ID, "");
                editor.putString(Constants.RECEIVING_ID_RAW, "");
                //TODO may want to clear other preferences as well as those are deprecated
                editor.commit();

                LocationRecord record = userRecords.get(0);
                if (record == null || !record.getRecId().equals(recId)) {
                    userRecords.add(new LocationRecord(0, recId, safeId, "TrackR1"));
                    // Save upgraded REC IDs into file
                    Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_REC_IDS_FILE_NAME, Utility.createFinalJsonString(userRecords.get(0)));
                }
            }
            return;
        }


        try {
            JSONObject jsnobject = new JSONObject(recIdsJsonString);
            JSONArray jsonArray = jsnobject.getJSONArray("receiverids");

            for (int i = 0; i < jsonArray.length(); i++) {
                SettingsObject settingsObject = Utility.createSettingsObjectFromJson((JSONObject) jsonArray.get(i));
                if (settingsObject != null) {
                    if (userRecords == null) {
                        userRecords.add(new LocationRecord(i, settingsObject.getId(), settingsObject.getSafeId(),
                                settingsObject.getAlias()));
                    } else {
                        if (userRecords.size() > i) {
                            // if userRecord exist keep it and update missing ones
                            LocationRecord record = userRecords.get(i);
                            if (record != null) {
                                if (record.getRecId().equals("")) {
                                    userRecords.get(i).setRecId(settingsObject.getId());
                                    userRecords.get(i).setSafeId(settingsObject.getSafeId());
                                    userRecords.get(i).setAlias(settingsObject.getAlias());
                                    continue;
                                }
                                if (record.getRecId().equals(settingsObject.getId())) {
                                    continue;
                                }
                            }
                        }
                        userRecords.add(new LocationRecord(i, settingsObject.getId(), settingsObject.getSafeId(), settingsObject.getAlias()));
                    }
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "#4# Error getting JSON obj or array. " + e.getMessage());
        }
    }


    private boolean checkIfshouldTryRetrieveDevicePosition() {
        if (userRecords == null || userRecords.size() == 0) {
            return false;
        }
        boolean shouldConnectToFB = true;

        // If I have location record and timestamp of last device upload is smalled than 15 minutes then I don't want to do update
        for (int i = 0; i < userRecords.size(); i++) {

            // if last update time is old we want to update
            long updateTimeout = Constants.FB_REQUEST_TIMEOUT;
            shouldConnectToFB = (userRecords.get(i).getTimestamp() + updateTimeout) < System.currentTimeMillis() - updateTimeout;
            // if location record doesn't exist for this id then we want to request data from server
            //shouldConnectToFB = true;
            if (shouldConnectToFB) {
                break;
            }
        }
        // connect to Firebase and update userRecords
        if (shouldConnectToFB) {
            getLastKnownLocation();
        }
        return shouldConnectToFB;
    }


    private void getLastKnownLocation() {
        if (isConnected) {
            // retrieve Location from FB for currently selected ID
            retrieveLocation();
        } else {
            Utility.showToast(getApplicationContext(), getString(R.string.no_connection));
            //enableSearchButton();
        }
    }

    private void retrieveLocation() {
        if (skipfbCallOnReconfiguration) {
            skipfbCallOnReconfiguration = false;
            return;
        }
        firebase = new Firebase("https://trackr1.firebaseio.com");
        firebase.goOnline();
        Log.d(TAG, "Firebase goes online");
        firebase.keepSynced(false);
        firebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        for (int i = 0; i < userRecords.size(); i++) {

                            if (snapshot.getKey().equals(userRecords.get(i).getSafeId())) {
                                // Processing received data
                                if (snapshot.hasChildren()) {
                                    Long idLong = ((Long) snapshot.child("id").getValue());
                                    double batteryLevel = -1;
                                    if (idLong != null) {
                                        batteryLevel = (double) snapshot.child("batteryLevel").getValue();
                                    }
                                    double latitude = (double) snapshot.child("latitude").getValue();
                                    double longitude = (double) snapshot.child("longitude").getValue();
                                    long timeStamp = (long) snapshot.child("timestamp").getValue();

                                    Log.i(Constants.TAG, "Recovered data from FB for id: " + i + " alias: " + userRecords.get(i).getAlias());

                                    // check if timestamps are same and if yes then don't
                                    // update loc record to save duplicate porcessing

                                    if (userRecords.get(i).getTimestamp() == timeStamp) {
                                        if (userRecords.get(i).getAddress().equals("")
                                                || userRecords.get(i).getAddress().equals(getResources().getString(R.string.address_not_found))
                                                || userRecords.get(i).getAddress().equals(getResources().getString(R.string.address_loc_error))) {
                                            getAddress(i);
                                        }
                                        continue;
                                    }

                                    // Store location and request addres translation
                                    userRecords.get(i).updateLocationRecord(latitude, longitude, timeStamp, batteryLevel);
                                    mAdapter.notifyItemChanged(i);
                                    getAddress(i);
                                    Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                                            Utility.createJsonArrayStringFromUserRecords(userRecords));
                                }

                            }
                        }
                    }
                    firebase.removeEventListener(this);
                    firebase.goOffline();
                    Log.i(Constants.TAG, "Firebase goes offline");

                    // Update location markers on the map.
                    showUsersLocationOnMap();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.i(Constants.TAG, "Update Cancelled" + firebaseError.getMessage());
                firebase.goOffline();
                Log.i(Constants.TAG, "Firebase goes offline");
            }
        });
    }

    private void showUpdateDialog() {
        String appVersion = Utility.getCurrentAppVersion(getApplicationContext());

        if (!preferences.getString(Constants.NEW_UPDATE, "2.0.15").equals(appVersion)) {
            //TODO uncomment this
            // Utility.createAlertDialog(MainActivity.this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.NEW_UPDATE, appVersion);
            editor.commit();
        }
    }

    private void showRateMyAppDialog() {

        if (preferences.getBoolean(Constants.RATING_POPUP_ENABLED, true)) {

            int counter = preferences.getInt(Constants.RATING_POPUP_COUNTER, 0);
            counter++;
            if (counter > Constants.RATING_POPUP_ATTEMPTS) {
                counter = 0;
                Utility.showRateMyAppDialog(MainActivityV2.this, preferences);
            }
            preferences.edit().putInt(Constants.RATING_POPUP_COUNTER, counter).commit();
        }
    }


    private void getAddress(int rowId) {
        registerAddressResolverReceiver();
        if (Geocoder.isPresent()) {
            Thread t = new Thread(new AddressResolverRunnable(getApplicationContext(), rowId, userRecords.get(rowId).getLatitude(),
                    userRecords.get(rowId).getLongitude()));
            t.setName("AddressResolverTrackR");
            t.setDaemon(true);
            t.start();
        } else {
            userRecords.get(rowId).setAddress(getResources().getString(R.string.not_available));
        }
    }

    private boolean refreshViewsIfChangeOccured(boolean showProgress) {

        if (preferences == null) {
            preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                    MODE_PRIVATE);
        }

        if (preferences.getBoolean(Constants.RECEIVING_ID_CHANGE, false)) {

            // TODO maybe I can wipe only changed ids and its location

            userRecords.clear();
            Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME, Utility.createJsonArrayStringFromUserRecords(userRecords));

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.RECEIVING_ID_CHANGE, false);
            editor.commit();
            // display loading screen
//            displayDeviceLocation(null, true);

            populateUserRecords();
            ((TrackListMainAdapter) mAdapter).updateViews(userRecords);
            // update views
//            mAdapter.notifyDataSetChanged();
            tryToRetrieveNewLocationWithProgress();
            return true;
        }
        return false;
    }

    private void tryToRetrieveNewLocationWithProgress() {
        if (checkIfshouldTryRetrieveDevicePosition()) {

        } else {
            Utility.showToast(getApplicationContext(), getString(R.string.last_location_fresh));
        }
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


    private void showUsersLocationOnMap(){
        if(mMap == null) {
            return;
        }
        mMap.clear();
        if (userRecords == null || userRecords.size() == 0) return; // this should not happen

        for (int i = 0; i < userRecords.size(); i++) {
            if (userRecords.get(i).getLatitude() != 0 || userRecords.get(i).getLongitude() != 0) {
                final LatLng location = new LatLng(userRecords.get(i).getLatitude(), userRecords.get(i).getLongitude());


                IconGenerator iconGenerator = new IconGenerator(getApplicationContext());
                iconGenerator.setStyle(i + 3);
                Bitmap bitmapMarker = iconGenerator.makeIcon(userRecords.get(i).getAlias() + "\n"
                        + Utility.parseDate(userRecords.get(i).getTimestamp()));

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(location)
                        .icon(BitmapDescriptorFactory
                                .fromBitmap(bitmapMarker))
                        .flat(true);

                mMap.addMarker(markerOptions);
                //add call back for switching user Records on the map
                ((TrackListMainAdapter) mAdapter).setItemSelectedListener(this);
            }
        }

        if (userRecords.get(currentTracker).getLatitude() != 0 || userRecords.get(currentTracker).getLongitude() != 0) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userRecords.get(currentTracker).getLatitude(),
                    userRecords.get(currentTracker).getLongitude()), 16f));
        } else {
            Utility.showToast(getApplicationContext(), "Ups nothing to show for " + userRecords.get(currentTracker).getAlias());

//            for (int i = 0; i < userRecords.size(); i++) {
//                if (userRecords.get(i).getLatitude() != 0 || userRecords.get(i).getLongitude() != 0) {
//                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userRecords.get(i).getLatitude(),
//                            userRecords.get(i).getLongitude()), 16f));
//                    break;
//                }
//            }
        }
    }


}

