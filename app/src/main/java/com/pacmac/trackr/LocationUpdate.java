package com.pacmac.trackr;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by pacmac on 2018-07-24.
 */

public class LocationUpdate implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "LocationUpdate";
    private static final long DELAY_LOCATION = 60 * 1000L;

    protected static LocationUpdate sInstance = null;

    private Context context = null;
    private GoogleApiClient mGoogleApiClient;
    private SharedPreferences preferences;
    private String trackingID = null;

    private static TrackLocationUpdateListener listener = null;
    private boolean isPermissionEnabled = true;

    private long lastLocationTime = 0L;
    private int updateFreq;

    public static LocationUpdate getLocationUpdateInstance(Context context, TrackLocationUpdateListener listener) {
        LocationUpdate.listener = listener;
        if (sInstance == null) {
            sInstance = new LocationUpdate(context);
        }
        return sInstance;
    }

    private LocationUpdate(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, Context.MODE_PRIVATE);
        initializeGPSandFirebase(context);
    }

    private void initializeGPSandFirebase(Context context) {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    protected void getLocation() {
        trackingID = preferences.getString(Constants.TRACKING_ID, "Error");
        if (trackingID.equals("Error")) {
            return;
        }

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && System.currentTimeMillis() > lastLocationTime + DELAY_LOCATION) {
            newLocation(getLastKnownLocation());
        }
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        newLocation(getLastKnownLocation());
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location lastLocation) {
    }

    private void newLocation(Location lastLocation) {
        if (lastLocation == null) {
            Log.e(TAG, "No Location retrieved from Google Play Services.");
            return;
        }
        long time = lastLocation.getTime();
        if (time == lastLocationTime) {
            return;
        }
        lastLocationTime = time;
        int batteryLevel = Utility.getBatteryLevel(context);
        int cellQuality = Utility.getCellSignalQuality(context, isPermissionEnabled);

        LocationTxObject newLocation = new LocationTxObject(lastLocation.getLatitude(),
                lastLocation.getLongitude(), time, batteryLevel, cellQuality);

        FirebaseHandler.fireUpload(newLocation, trackingID, listener);
    }
}
