package com.pacmac.trackr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pacmac.trackr.mapmarker.IconGenerator;
import com.tutelatechnologies.sdk.framework.TutelaSDKFactory;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by pacmac on 2017-08-05.
 */
public class MainActivityV2 extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener,
        NetworkStateListener, TrackListMainAdapter.TrackListItemSelectedListener, FirebaseHandler.FirebaseDownloadCompleteListener {

    private static final String TAG = "TrackRMain";

    SharedPreferences preferences = null;

    private GoogleMap mMap;
    private NetworkStateChangedReceiver connReceiver = null;
    private List<LocationRecord> userRecords = new ArrayList<>();

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private AppBarLayout appBarCollapsable;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private BottomNavigationView bottomNavigation;
    private LinearLayout noDeviceView;
    private FloatingActionButton fab;
    private TypedArray stockImages;

    private LinearLayout refreshPanel;
    private Button refreshButton;
    private TextView refreshTimeView;

    /**
     * show title must be true during initialization so it can be handled properly during collapse
     */
    private boolean showTitle = true;
    private boolean isConnected = false;
    private boolean skipConnReceiverTrigger = true;
    private boolean shouldAnimateMap = true;
    private boolean isPermissionEnabled = true;
    private boolean skipfbCallOnReconfiguration = false;
    private boolean isAddressResolverRegistred = false;
    private boolean isRefreshListHandlerRegistred = false;
    private boolean isFirstAppRun = false;
    private boolean shouldShowObsoleteNotification = true;

    private int currentTracker = 0;
    private int refreshCounter = 0;
    private int FIRST_RUN_FETCH_DELAY = 5 * 1000;
    private int REFRESH_DELAY = 60 * 1000;
    private int REFRESH_DELAY_SHORT = 10 * 1000;

    private long lastFbCheckTimestamp = 0;

    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                TutelaSDKFactory.getTheSDK().initializeWithApiKey(Constants.REG_KEY, this, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        bottomNavigation = findViewById(R.id.navigation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));

        stockImages = getResources().obtainTypedArray(R.array.stockImages);

        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                MODE_PRIVATE);
        isPermissionEnabled = Utility.checkSelfPermission(getApplicationContext(), LOCATION_PERMISSION);

        shouldShowObsoleteNotification = preferences.getBoolean(Constants.OBSOLETE_INFO, true);
        if (!shouldShowObsoleteNotification && Utility.getDayOfMonth() < 23) {
            shouldShowObsoleteNotification = true;
            preferences.edit().putBoolean(Constants.OBSOLETE_INFO, shouldShowObsoleteNotification).apply();
        }

        isFirstAppRun = preferences.getBoolean(Constants.FIRST_RUN, true);
        if (isFirstAppRun) {
            createDefaultIdsAndMyPhoneRow();
            if (!isPermissionEnabled) {
                showDialogForUserToEnableTracking();
            }
        } else {
            // disable tracking state if permission was disabled while app was in background
            if (!isPermissionEnabled) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Constants.TRACKING_STATE, false);
                editor.commit();
                Utility.showToast(getApplicationContext(), "Tracking Mode Disabled. Check your app settings.",
                        ((View) bottomNavigation).getHeight(), false);
            }
            loadUserRecordsFromFile();
        }

        // restore location on reconfiguration
        if (savedInstanceState != null) {
            currentTracker = savedInstanceState.getInt(Constants.KEY_ITEM_ORDER, 0);
            skipfbCallOnReconfiguration = true;
        }


        connReceiver = new NetworkStateChangedReceiver();
        connReceiver.setConnectionListener(this);

        noDeviceView = findViewById(R.id.emptyListView);
        mRecyclerView = findViewById(R.id.trackList);
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new TrackListMainAdapter(userRecords, getApplicationContext(), shouldShowObsoleteNotification);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setVerticalScrollBarEnabled(false);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                Log.e(TAG, " DX:" + dx + " DY:" + dy);
            }
        });

        //add call back for switching user Records on the map
        ((TrackListMainAdapter) mAdapter).setItemSelectedListener(this);


        appBarCollapsable = findViewById(R.id.appBarCollapsable);

        isConnected = Utility.checkConnectivity(getApplicationContext());
        Utility.startTrackingService(getApplicationContext(), preferences);
        showUpdateDialog();
        showRateMyAppDialog();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().add(R.id.map_container, mapFragment).commit();
        mapFragment.getMapAsync(this);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utility.openUserEditActivity(MainActivityV2.this, -1, userRecords);
            }
        });

        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                handleBottomNavigationItemSelected(item);
                return true;
            }
        });

        bottomNavigation.getMenu().getItem(0).setCheckable(false);
        bottomNavigation.getMenu().getItem(1).setCheckable(false);
        bottomNavigation.getMenu().getItem(2).setCheckable(false);

        if (userRecords.size() == 0) {
            noDeviceView.setVisibility(View.VISIBLE);
            appBarCollapsable.setExpanded(false);
        }


        //hide show title text
        appBarCollapsable.addOnOffsetChangedListener(new AppBarStateChangeListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, State state, int alpha) {
                if (state == State.EXPANDED) {
                    //hide title
                    showTitle = true;
                    collapsingToolbarLayout.setTitle("");
                } else if (state == State.COLLAPSED) {
                    //show title
                    showTitle = true;
                    collapsingToolbarLayout.setTitle(getString(R.string.app_name));
                } else {
                    //hide title
                    if (showTitle) {
                        collapsingToolbarLayout.setTitle("");
                        showTitle = false;
                    }
                }
            }
        });


        // Disable "Drag" for AppBarLayout (i.e. User can't scroll appBarLayout by directly touching appBarLayout - User can only scroll appBarLayout by only using scrollContent)
        if (appBarCollapsable.getLayoutParams() != null) {
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) appBarCollapsable.getLayoutParams();
            AppBarLayout.Behavior appBarLayoutBehaviour = new AppBarLayout.Behavior();
            appBarLayoutBehaviour.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                @Override
                public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                    return false;
                }
            });
            layoutParams.setBehavior(appBarLayoutBehaviour);
            //SET MAP to 3/5 of screen if expanded
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            layoutParams.height = 3 * size.y / 5;
        }


        // REFRESH PANEL
        refreshPanel = findViewById(R.id.refreshPanel);
        refreshTimeView = findViewById(R.id.refreshTimeView);
        refreshButton = findViewById(R.id.refreshBtn);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshCounter = 0;
                lastFbCheckTimestamp = 0;
                refreshPanel.setVisibility(View.GONE);
                if (userRecords.size() > 0 && checkIfshouldTryRetrieveDevicePosition()) {
                    startRefreshListTimer(REFRESH_DELAY);
                } else {
                    startRefreshListTimer(REFRESH_DELAY_SHORT);
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerAddressResolverReceiver();

        Utility.stopFetchingService(getApplicationContext());

        shouldAnimateMap = true;
        refreshCounter = 0;
        if (userRecords.size() > 0 && Utility.checkPlayServices(this)) {
            appBarCollapsable.setExpanded(true);
        }
        enableButtonsInNavBar();
        skipConnReceiverTrigger = true;
        registerReceiver(connReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        if (isPermissionEnabled) {
            Utility.startTrackingService(getApplicationContext(), preferences);
        }
        showUsersLocationOnMap(shouldAnimateMap);

        refreshCounter = 0;
        refreshPanel.setVisibility(View.GONE);
        if (userRecords.size() > 0 && checkIfshouldTryRetrieveDevicePosition()) {
            if (isFirstAppRun) {
                isFirstAppRun = false;
                startRefreshListTimer(FIRST_RUN_FETCH_DELAY);
            } else {
                startRefreshListTimer(REFRESH_DELAY);
            }
        } else {
            startRefreshListTimer(REFRESH_DELAY_SHORT);
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
        if (isRefreshListHandlerRegistred) {
            stopRefreshListTimer();
        }
        if (userRecords.size() > 0) {
            Utility.startFetchingService(getApplicationContext());
        } else {
            Utility.stopFetchingService(getApplicationContext());
        }
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
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        showUsersLocationOnMap(true);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (userRecords != null && userRecords.size() != 0) {
            outState.putInt(Constants.KEY_ITEM_ORDER, currentTracker);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void connectionChanged(boolean isConnected) {
        Log.d(Constants.TAG, "Conn changed: " + isConnected);
        this.isConnected = isConnected;
        if (isConnected && !skipConnReceiverTrigger) {
            refreshCounter = 0;
            refreshPanel.setVisibility(View.GONE);
            if(checkIfshouldTryRetrieveDevicePosition() && refreshPanel.getVisibility() == View.VISIBLE) {
                lastFbCheckTimestamp = 0;
            }
        }
        skipConnReceiverTrigger = false;
    }

    @Override
    public void OnItemSelected(int position) {
        currentTracker = position;
        if (mMap != null) {
            appBarCollapsable.setExpanded(true, true);
            if (userRecords.get(currentTracker).getLatitude() != 0 || userRecords.get(currentTracker).getLongitude() != 0) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userRecords.get(currentTracker).getLatitude(),
                        userRecords.get(currentTracker).getLongitude()), 16f));
            } else {
                Utility.showToast(getApplicationContext(), "Ups nothing to show for " +
                        userRecords.get(currentTracker).getAlias(), ((View) bottomNavigation).getHeight(), false);
            }
        }
    }

    @Override
    public void OnItemEditClicked(int position) {
        Utility.openUserEditActivity(this, position, userRecords);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {

        Log.d(TAG, "Activity Result :" + requestCode);
        if (requestCode == Constants.EDIT_RESULT_REQUEST_CODE) {

            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Activity Result OK");
                boolean retrieveLocations = false;
                int position = resultIntent.getIntExtra(Constants.EDIT_USER_POSITION, -10);

                String alias = resultIntent.getStringExtra(Constants.EDIT_USER_ALIAS);
                String id = resultIntent.getStringExtra(Constants.EDIT_USER_ID);
                int profileImageId = resultIntent.getIntExtra(Constants.EDIT_USER_IMG, 0);
//                  int img = resultIntent.getIntExtra(Constants.EDIT_USER_IMG, -1);
                // if position is = -1 then it is very new record
                if (position == -1) {
                    userRecords.add(new LocationRecord(-1, id, Utility.checkAndReplaceForbiddenChars(id), alias, profileImageId));
                    retrieveLocations = true;
                    mAdapter.notifyItemInserted(userRecords.size() - 1);
                } else {
                    userRecords.get(position).setAlias(alias);
                    userRecords.get(position).setProfileImageId(profileImageId);
                    if (!userRecords.get(position).getRecId().equals(id)) {
                        userRecords.get(position).setRecId(id);
                        userRecords.get(position).setSafeId(Utility.checkAndReplaceForbiddenChars(id));
                        userRecords.get(position).resetParams();
                        retrieveLocations = true;
                    }
                    mAdapter.notifyItemChanged(position);
                    // update img resource
                }
                Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                        Utility.createJsonArrayStringFromUserRecords(userRecords));

                if (retrieveLocations) {
                    getLastKnownLocation();
                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "Activity RESULT_CANCELED");
            } else if (resultCode == Constants.EDIT_DELETE_POSITION) {
                Log.d(TAG, "Activity EDIT_DELETE_POSITION");
                int position = resultIntent.getIntExtra(Constants.EDIT_USER_POSITION, -10);
                if (position >= 0) {
                    userRecords.remove(position);
                    mAdapter.notifyItemRemoved(position);
                }
                Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                        Utility.createJsonArrayStringFromUserRecords(userRecords));
            }
        } else if (requestCode == Constants.SETTINGS_REQUESTCODE) {
            if (resultCode == Constants.SETTINGS_UPDATE_RESULT) {
                Log.d(TAG, "Activity result: SETTINGS_UPDATE_RESULT");
                loadUserRecordsFromFile();
                ((TrackListMainAdapter) mAdapter).updateViews(userRecords);
            }
        }

        // Display no item in list image and message
        if (userRecords.size() == 0) {
            noDeviceView.setVisibility(View.VISIBLE);
            appBarCollapsable.setExpanded(false);
            Log.d(TAG, "no device view - VISIBLE");

        } else {
            Log.d(TAG, "no device view - GONE");
            noDeviceView.setVisibility(View.GONE);
        }
    }

    /**
     * Methods
     */
    private void handleBottomNavigationItemSelected(MenuItem item) {
        item.setEnabled(false);
        switch (item.getItemId()) {
            case R.id.navigation_about:
                Intent i = new Intent(getApplicationContext(), HelpActivity.class);
                startActivity(i);
                break;
            case R.id.navigation_share:
                onInviteClicked();
                //item.setEnabled(true);
                break;
            case R.id.navigation_settings:
                Utility.openSettings(getApplicationContext(), MainActivityV2.this);
                break;
        }
    }

    private void onInviteClicked() {
        Intent intent = new AppInviteInvitation.IntentBuilder("Android TrackeR")
                .setMessage(getApplicationContext().getString(R.string.invite_subject))
                .setDeepLink(Uri.parse("https://play.google.com/store/apps/details?id=com.pacmac.trackrr"))
                .setCallToActionText(getApplicationContext().getString(R.string.invite_action))
                .build();
        startActivityForResult(intent, 8213);
    }

    private void createDefaultIdsAndMyPhoneRow() {

        SharedPreferences.Editor editor = preferences.edit();
        String trackId = Utility.generateUniqueID().substring(0, 10);
        editor.putString(Constants.TRACKING_ID, trackId);
        editor.putString(Constants.TRACKING_ID_RAW, trackId);
        //Enable tracking of this phone on start
        editor.putBoolean(Constants.TRACKING_STATE, isPermissionEnabled);
        editor.putBoolean(Constants.FIRST_RUN, false);
        editor.commit();

        if (isPermissionEnabled) {
            Log.d(TAG, "IN createDefaultIdsAndMyPhoneRow");
            userRecords = Utility.convertJsonStringToUserRecords(getFilesDir() + Constants.JSON_LOC_FILE_NAME);
            userRecords.add(new LocationRecord(-10, trackId, trackId, "My Phone", -1));
            Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                    Utility.createJsonArrayStringFromUserRecords(userRecords));
        }
    }

    private void loadUserRecordsFromFile() {
        userRecords = Utility.convertJsonStringToUserRecords(getFilesDir() + Constants.JSON_LOC_FILE_NAME);
        Log.d(TAG, "userRecords.size: " + userRecords.size());
        if (userRecords.size() > 0 && !userRecords.get(0).getRecId().equals("")) {
            return;
        }

        // To support deprecated code I need to use the rest of this method.
        String recIdsJsonString = Utility.loadJsonStringFromFile(getFilesDir() + Constants.JSON_REC_IDS_FILE_NAME);

        String recId = preferences.getString(Constants.RECEIVING_ID, "");
        String safeId = preferences.getString(Constants.RECEIVING_ID_RAW, "");

        if (recIdsJsonString.equals("")) {
            // file doesn't exist
            //backward compatibility

            // TODO this piece might be removed later as it is only for pre v2 upgrades
            if (!recId.equals("")) {
                // we likely upgraded from older version delete pref here
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(Constants.RECEIVING_ID, "");
                editor.putString(Constants.RECEIVING_ID_RAW, "");
                //TODO may want to clear other preferences as well as those are deprecated
                editor.commit();

                if (userRecords.size() == 0 || userRecords.get(0) == null || !userRecords.get(0).getRecId().equals(recId)) {
                    userRecords.add(new LocationRecord(0, recId, safeId, "TrackR1", -1));
                    // Save upgraded REC IDs into file
                    Utility.saveJsonStringToFile(getFilesDir() +
                            Constants.JSON_REC_IDS_FILE_NAME, Utility.createFinalJsonString(userRecords.get(0)));
                }
            }
            return;
        }

        try {
            JSONObject jsnobject = new JSONObject(recIdsJsonString);
            JSONArray jsonArray = jsnobject.getJSONArray("receiverids");
            String trackIDRaw = preferences.getString(Constants.TRACKING_ID_RAW, "Error #5#");
            boolean trackingState = preferences.getBoolean(Constants.TRACKING_STATE, false);
            boolean isMyPhoneFound = false;

            for (int i = 0; i < jsonArray.length(); i++) {
                SettingsObject settingsObject = Utility.createSettingsObjectFromJson((JSONObject) jsonArray.get(i));
                if (settingsObject != null) {
                    if (userRecords.size() == 0) {

                        if (settingsObject.getId().equals(trackIDRaw) && trackingState) {
                            Log.d(TAG, "Adding user1: " + settingsObject.getAlias());
                            userRecords.add(new LocationRecord(-10, settingsObject.getId(), settingsObject.getSafeId(), settingsObject.getAlias(), -1));
                            isMyPhoneFound = true;
                            continue;
                        }
                        Log.d(TAG, "Adding user2: " + settingsObject.getAlias());
                        userRecords.add(new LocationRecord(i, settingsObject.getId(), settingsObject.getSafeId(),
                                settingsObject.getAlias(), -1));
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
                        Log.d(TAG, "Adding user3: " + settingsObject.getAlias());
                        userRecords.add(new LocationRecord(i, settingsObject.getId(), settingsObject.getSafeId(), settingsObject.getAlias(), -1));
                    }
                }
            }
            if (!isMyPhoneFound) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Constants.MY_PHONE_IN_LIST, false);
                editor.commit();
            }
            File obsoleteFile = new File(getFilesDir() + Constants.JSON_REC_IDS_FILE_NAME);
            obsoleteFile.delete();
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
            Utility.showToast(getApplicationContext(), getString(R.string.no_connection),
                    ((View) bottomNavigation).getHeight(), false);
            //enableSearchButton();
        }
    }

    private void retrieveLocation() {
        if (skipfbCallOnReconfiguration) {
            skipfbCallOnReconfiguration = false;
            return;
        }
        lastFbCheckTimestamp = System.currentTimeMillis();
        FirebaseHandler.fetchFirebaseData(getApplicationContext(), userRecords, this);
    }

    private void showUpdateDialog() {
        String appVersion = Utility.getCurrentAppVersion(getApplicationContext());

        if (!preferences.getString(Constants.NEW_UPDATE, appVersion).equals(appVersion)
                && !preferences.getString(Constants.NEW_UPDATE, "3.5.0").equals(appVersion)) {
            Utility.createAlertDialog(MainActivityV2.this);
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

    private void showUsersLocationOnMap(boolean showUserOnMap) {
        if (mMap == null) {
            return;
        }
        mMap.clear();
        if (userRecords == null || userRecords.size() == 0) return; // this should not happen

        for (int i = 0; i < userRecords.size(); i++) {
            if (userRecords.get(i).getLatitude() != 0 || userRecords.get(i).getLongitude() != 0) {
                final LatLng location = new LatLng(userRecords.get(i).getLatitude(), userRecords.get(i).getLongitude());

                IconGenerator iconGenerator = new IconGenerator(getApplicationContext());
                iconGenerator.setStyle(i + 3);
//                iconGenerator.setBackground();

                if (userRecords.get(i).getProfileImageId() >= stockImages.length()
                        || userRecords.get(i).getProfileImageId() < 0) {
                    userRecords.get(i).setProfileImageId(0);
                }
                Bitmap bitmapMarker;
                try {
                    bitmapMarker = iconGenerator.makeIcon(stockImages
                            .getResourceId(userRecords.get(i).getProfileImageId(), 0));
                } catch (Exception e) {
                    bitmapMarker = iconGenerator.makeIcon(stockImages
                            .getResourceId(0, 0));
                }

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(location)
                        .icon(BitmapDescriptorFactory
                                .fromBitmap(bitmapMarker))
                        .flat(true);

                mMap.addMarker(markerOptions);
            }
        }

        // make sure if do not try to display deleted device
        if (currentTracker >= userRecords.size()) {
            currentTracker = 0;
        }
        if (showUserOnMap && (userRecords.get(currentTracker).getLatitude() != 0 || userRecords.get(currentTracker).getLongitude() != 0)) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userRecords.get(currentTracker).getLatitude(),
                    userRecords.get(currentTracker).getLongitude()), 14f));
        }
    }

    /**
     * This call back only sets boolean flag that user interacted with Map
     * fragment and will turn off animation of marker position on each refresh.
     *
     * @param latLng
     */
    @Override
    public void onMapClick(LatLng latLng) {
        shouldAnimateMap = false;
    }


    private void showDialogForUserToEnableTracking() {

        final Dialog dialog = new Dialog(MainActivityV2.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_default);
        dialog.setCancelable(false);

        TextView title = dialog.findViewById(R.id.title);
        title.setText(getString(R.string.dialog_title_track_enable));
        TextView content = dialog.findViewById(R.id.content);
        content.setText(getString(R.string.dialog_content_track_enable));

        Button yesButton = dialog.findViewById(R.id.dialogYes);
        yesButton.setText(R.string.dialog_setup);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utility.openSettings(getApplicationContext(), MainActivityV2.this);
                dialog.dismiss();
            }
        });

        Button noButton = dialog.findViewById(R.id.dialogCancel);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    private void enableButtonsInNavBar() {
        bottomNavigation.getMenu().getItem(0).setEnabled(true);
        bottomNavigation.getMenu().getItem(1).setEnabled(true);
        bottomNavigation.getMenu().getItem(2).setEnabled(true);
    }


    /**
     * Refreshing the List View to make sure the time in view is updated and will attempt to poll new data as well
     */
    private Handler refreshListHandler = new Handler();
    private Runnable refreshListRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "refreshing LIST");
            mAdapter.notifyDataSetChanged();

            int delay = REFRESH_DELAY;

            // Only reach firebase 2 times after resume
            if (refreshCounter < 1) {
                if (checkIfshouldTryRetrieveDevicePosition()) {
                    refreshCounter++;
                    delay = REFRESH_DELAY_SHORT;
                }
            } else if (refreshPanel.getVisibility() == View.GONE && lastFbCheckTimestamp != 0) {
                if (refreshCounter >= 1) {
                    refreshPanel.setVisibility(View.VISIBLE);
//                    ScaleAnimation anim = new ScaleAnimation(1,1,0,1);
//                    anim.setDuration(1000);
//                    anim.setFillAfter(true);
//                    refreshPanel.startAnimation(anim);

                    refreshTimeView.setText(Utility.getLastFBPullTime(lastFbCheckTimestamp));
                }
                refreshCounter++;
            }
            startRefreshListTimer(delay);
        }
    };



    private void startRefreshListTimer(int delay) {
        isRefreshListHandlerRegistred = true;
        refreshListHandler.postDelayed(refreshListRunnable, delay);
    }

    private void stopRefreshListTimer() {
        isRefreshListHandlerRegistred = false;
        refreshListHandler.removeCallbacks(refreshListRunnable);
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void onDownloadCompleteListener(int row) {
        mAdapter.notifyItemChanged(row);
    }

    @Override
    public void updateMap() {
        showUsersLocationOnMap(shouldAnimateMap);
    }
}

