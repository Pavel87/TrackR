package com.pacmac.trackr;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by pacmac on 28/04/16.
 */

public class LocationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, Firebase.CompletionListener {

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Firebase firebase;
    private SharedPreferences preferences;
    private String child = null;
    private boolean lastBatLevel = false;

    private LocationTxObject locationTxObject= null;

    @Override
    public void onCreate() {
        super.onCreate();

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);
        child = preferences.getString(Constants.TRACKING_ID, "Error");
        if (child.equals("Error")) stopSelf();

        mGoogleApiClient.connect();
        Log.d(Constants.TAG, "LocationService Started");
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
        Log.d(Constants.TAG, "LocationService is destroying");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createLocationRequest(long time) {
        mLocationRequest = new LocationRequest().create();
        mLocationRequest.setInterval(time);
        mLocationRequest.setFastestInterval(time);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        float level = getBatteryLevel();
        if (level >= 25) {
            createLocationRequest(Constants.TIME_BATTERY_OK);
            Log.d(Constants.TAG, "Battery OK: " + level);
            lastBatLevel = true;
        } else {
            createLocationRequest(Constants.TIME_BATTERY_LOW);
            Log.d(Constants.TAG, "Battery LOW: " + level);
            lastBatLevel = false;
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location lastLocation) {
        long time = lastLocation.getTime();
        double batteryLevel = Math.round(getBatteryLevel() * 100.0) / 100.0;
        firebase = new Firebase("https://trackr1.firebaseio.com");
        firebase.keepSynced(false);
        firebase.goOnline();
        Log.d(Constants.TAG, "Firebase goes online - attempt to update location");
        firebase.child(child).setValue(new LocationTxObject(0, lastLocation.getLatitude(),
                lastLocation.getLongitude(), time, batteryLevel), this);
        if (batteryLevel >= 25 && !lastBatLevel) {
            lastBatLevel = true;
            stopLocationUpdates();
            createLocationRequest(Constants.TIME_BATTERY_OK);
            startLocationUpdates();
        } else if (batteryLevel < 25 && lastBatLevel) {
            lastBatLevel = false;
            stopLocationUpdates();
            createLocationRequest(Constants.TIME_BATTERY_LOW);
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return 51.0f;
        }
        return ((float) level / (float) scale) * 100.0f;
    }

    @Override
    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
        Log.d(Constants.TAG, "TrackR finished server upload");

        if(firebaseError != null) {
            Log.d(Constants.TAG, firebaseError.getDetails() + " Message: " + firebaseError.getMessage());
        }

        if (firebase != null){
            firebase.goOffline();
            Log.d(Constants.TAG, "Firebase goes offline");
        }
    }
}
