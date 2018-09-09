package com.pacmac.trackr;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by pacmac on 28/04/16.
 */

public class LocationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener { //Firebase.CompletionListener

    private static final String TAG = "LocServ";

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private SharedPreferences preferences;
    private String trackingID = null;
    private boolean lastBatLevel = false;

    private boolean isPermissionEnabled = true;

    private int updateFreq = Constants.TIME_BATTERY_OK * 60 * 1000;
    private int updateFreqLowBat = updateFreq + 25 * 60 * 1000;
    private long lastLocationTime = 0L;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        isPermissionEnabled = Utility.checkSelfPermission(getApplicationContext(), Constants.LOCATION_PERMISSION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);

        if (!isPermissionEnabled) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        updateLocFreqTime();
        trackingID = preferences.getString(Constants.TRACKING_ID, "Error");
        if (trackingID.equals("Error")) stopSelf();

        mGoogleApiClient.connect();
        Log.d(TAG, "LocationService Started");
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
        Log.d(TAG, "LocationService is destroying");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createLocationRequest(long time) {
        mLocationRequest = new LocationRequest().create();
        mLocationRequest.setInterval(time);
        mLocationRequest.setFastestInterval(time / 5);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    private void startLocationUpdates() {
        if (!Utility.checkSelfPermission(getApplicationContext(), Constants.LOCATION_PERMISSION)) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    private void updateLocFreqTime() {
        updateFreq = preferences.getInt(Constants.TRACKING_FREQ, Constants.TIME_BATTERY_OK) * 60 * 1000;
        updateFreqLowBat = updateFreq + 25 * 60 * 1000; // low bat is + 25 minutes
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        int level = Utility.getBatteryLevel(getApplicationContext());
        if (level >= 25) {
            createLocationRequest(updateFreq);
            Log.d(TAG, "Battery OK: " + level);
            lastBatLevel = true;
        } else {
            createLocationRequest(updateFreqLowBat);
            Log.d(TAG, "Battery LOW: " + level);
            lastBatLevel = false;
        }
        processNewLocation(getLastKnownLocation());
        startLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onLocationChanged(Location lastLocation) {
        Log.d(TAG, "TRACKR retrieved new LOCATION.");
        processNewLocation(lastLocation);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    private void processNewLocation(Location lastLocation) {
        if (lastLocation == null) {
            return;
        }
        long time = lastLocation.getTime();
        if (time == lastLocationTime) {
            Log.d(TAG, "Location same as previous. SKIP");
            return;
        }
        lastLocationTime = time;
        int batteryLevel = Utility.getBatteryLevel(getApplicationContext());
        int cellQuality = Utility.getCellSignalQuality(getApplicationContext(), isPermissionEnabled);
        LocationTxObject newLocation = new LocationTxObject(lastLocation.getLatitude(),
                lastLocation.getLongitude(), time, batteryLevel, cellQuality);

        FirebaseHandler.fireUpload(newLocation, trackingID, null);

        if (batteryLevel >= 25 && !lastBatLevel) {
            updateLocFreqTime();
            lastBatLevel = true;
            stopLocationUpdates();
            createLocationRequest(updateFreq);
            startLocationUpdates();
        } else if (batteryLevel < 25 && lastBatLevel) {
            updateLocFreqTime();
            lastBatLevel = false;
            stopLocationUpdates();
            createLocationRequest(updateFreqLowBat);
            startLocationUpdates();
        }
    }
}
