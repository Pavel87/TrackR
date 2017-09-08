package com.pacmac.trackr;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pacmac on 28/04/16.
 */

public class LocationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener { //Firebase.CompletionListener

    private static final String TAG = "LocServ";
    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private FirebaseDatabase database;
    private DatabaseReference dbReference;
    private SharedPreferences preferences;
    private String child = null;
    private boolean lastBatLevel = false;

    private boolean isPermissionEnabled = true;

    private int updateFreq = Constants.TIME_BATTERY_OK * 60 * 1000;;
    private int updateFreqLowBat = updateFreq + 25 * 60 * 1000;;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        isPermissionEnabled = Utility.checkSelfPermission(getApplicationContext(), Constants.LOCATION_PERMISSION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);

        if(!isPermissionEnabled) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.TRACKING_STATE, false);
            editor.commit();

            stopSelf();
            return START_NOT_STICKY;
        }

        database = FirebaseDatabase.getInstance();
        dbReference = database.getReferenceFromUrl("https://trackr1.firebaseio.com/");

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        updateLocFreqTime();
        child = preferences.getString(Constants.TRACKING_ID, "Error");
        if (child.equals("Error")) stopSelf();

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
        mLocationRequest.setFastestInterval(time);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    private void startLocationUpdates() {
        if(!Utility.checkSelfPermission(getApplicationContext(), Constants.LOCATION_PERMISSION)) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    private void updateLocFreqTime(){
        updateFreq = preferences.getInt(Constants.TRACKING_FREQ, Constants.TIME_BATTERY_OK) * 60 * 1000;
        updateFreqLowBat = updateFreq +25 * 60 * 1000; // low bat is + 25 minutes
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        float level = getBatteryLevel();
        if (level >= 25) {
            createLocationRequest(updateFreq);
            Log.d(TAG, "Battery OK: " + level);
            lastBatLevel = true;
        } else {
            createLocationRequest(updateFreqLowBat);
            Log.d(TAG, "Battery LOW: " + level);
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
        int cellQuality = getCellSignalQuality(getApplicationContext());
        dbReference.goOnline();
        Log.d(TAG, "Firebase goes online - attempt to update location");
//        firebase.keepSynced(false);
//        firebase.goOnline();

        LocationTxObject newLocation = new LocationTxObject(lastLocation.getLatitude(),
                lastLocation.getLongitude(), time, batteryLevel, cellQuality);
        Map<String, Object> locationUpdateMap = new HashMap<>();

        locationUpdateMap.put(String.valueOf(time), newLocation.createMap());
        dbReference.child(child).updateChildren(locationUpdateMap);

        dbReference.child(child).child("batteryLevel").setValue(newLocation.getBatteryLevel()+0.01);
        dbReference.child(child).child("latitude").setValue(newLocation.getLatitude());
        dbReference.child(child).child("longitude").setValue(newLocation.getLongitude());
        dbReference.child(child).child("timestamp").setValue(time);
        dbReference.child(child).child("id").setValue(1);

//        firebase.child(child).child(String.valueOf(time)).setValue(new LocationTxObject(lastLocation.getLatitude(),
//                lastLocation.getLongitude(), time, batteryLevel, cellQuality), this);
//        firebase.child(child).setValue(new LocationTxObject(lastLocation.getLatitude(),
//                lastLocation.getLongitude(), time, batteryLevel, cellQuality), this);
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
        //dbReference.goOffline();
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

//    @Override
//    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
//        Log.d(TAG, "TrackR finished server upload");
//
//        if(firebaseError != null) {
//            Log.d(TAG, firebaseError.getDetails() + " Message: " + firebaseError.getMessage());
//        }
//
//        if (firebase != null){
//            firebase.goOffline();
//            Log.d(TAG, "Firebase goes offline");
//        }
//    }

    private int getCellSignalQuality(Context context) {
        int cellQuality = -1;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1){
            return cellQuality;
        }

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return cellQuality;
        }

        if (!isPermissionEnabled) {
            return cellQuality;
        }
        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
        if(cellInfoList == null){
            return cellQuality;
        }
        for (CellInfo cell : cellInfoList) {
            if(cell.isRegistered()){
                if (cell instanceof CellInfoLte) {
                    cellQuality = ((CellInfoLte) cell).getCellSignalStrength().getLevel();
                } else if (cell instanceof CellInfoWcdma) {
                    cellQuality = ((CellInfoWcdma) cell).getCellSignalStrength().getLevel();
                } else if(cell instanceof CellInfoGsm) {
                    cellQuality = ((CellInfoGsm) cell).getCellSignalStrength().getLevel();
                } else if (cell instanceof CellInfoCdma) {
                    cellQuality = ((CellInfoCdma) cell).getCellSignalStrength().getLevel();
                }
                break;
            }
        }
        return cellQuality;
    }

}
