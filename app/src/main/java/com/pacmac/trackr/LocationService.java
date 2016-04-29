package com.pacmac.trackr;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by pacmac on 28/04/16.
 */

public class LocationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Firebase firebase;
    private SharedPreferences preferences;
    private String child = null;

    @Override
    public void onCreate() {
        super.onCreate();
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

        Firebase.setAndroidContext(getApplicationContext());
        firebase = new Firebase("https://trackr1.firebaseio.com");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mGoogleApiClient.connect();
        Log.d(Constants.TAG, "LocationService Started");
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        Log.d(Constants.TAG, "LocationService is destroying");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(60 * 60 * 1000);
        mLocationRequest.setFastestInterval(10 * 60 * 1000);
        mLocationRequest.setSmallestDisplacement(100);
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
        createLocationRequest();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location lastLocation) {
        long time = lastLocation.getTime();
        //String mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        //System.out.println(mLastUpdateTime + "  " + lastLocation.getLatitude() + " " + lastLocation.getLongitude() + " || " + accuracy);
        firebase.child(child).setValue(new LocationRecord(lastLocation.getLatitude(), lastLocation.getLongitude(), time));

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

}
