package com.pacmac.trackr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
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
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by pacmac on 2018-07-24.
 */

public class LocationUpdate implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "LocationUpdate";
    private static final long DELAY_LOCATION = 60*1000L;

    protected static LocationUpdate sInstance = null;

    private Context context = null;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseDatabase database;
    private DatabaseReference dbReference;
    private SharedPreferences preferences;
    private String child = null;

    private static TrackLocationUpdateListener listener = null;
    private boolean isPermissionEnabled = true;

    private long lastLocationTime = 0L;
    private int updateFreq;


    public static LocationUpdate getLocationUpdateInstance(Context context, TrackLocationUpdateListener listener) {
        LocationUpdate.listener = listener;
        if(sInstance == null) {
            sInstance = new LocationUpdate(context);
        }
        return sInstance;
    }

    private LocationUpdate(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, Context.MODE_PRIVATE);
        updateFreq = preferences.getInt(Constants.TRACKING_FREQ, Constants.TIME_BATTERY_OK) * 60 * 1000;
        initializeGPSandFirebase();
    }

    private void initializeGPSandFirebase() {
        try {
            database = FirebaseDatabase.getInstance();
            dbReference = database.getReferenceFromUrl("https://trackr1.firebaseio.com/");
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        child = preferences.getString(Constants.TRACKING_ID, "Error");
        if (child.equals("Error")) return;
        mGoogleApiClient.connect();
    }

    protected void getLocation() {
        if(mGoogleApiClient.isConnected() && System.currentTimeMillis() > lastLocationTime + DELAY_LOCATION) {
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
        dbReference.goOffline();
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
        lastLocationTime = time;
        double batteryLevel = Math.round(getBatteryLevel() * 100.0) / 100.0;
        int cellQuality = getCellSignalQuality(context);
        dbReference.goOnline();
        Log.d(TAG, "Firebase goes online - attempt to update location");
        dbReference.keepSynced(false);

        LocationTxObject newLocation = new LocationTxObject(lastLocation.getLatitude(),
                lastLocation.getLongitude(), time, batteryLevel, cellQuality);

        dbReference.child(child).child("batteryLevel").setValue(newLocation.getBatteryLevel() + 0.01);
        dbReference.child(child).child("latitude").setValue(newLocation.getLatitude());
        dbReference.child(child).child("longitude").setValue(newLocation.getLongitude());
        dbReference.child(child).child("timestamp").setValue(time);
        dbReference.child(child).child("cellQuality").setValue(cellQuality);
        dbReference.child(child).child("id").setValue(2);
        dbReference.goOffline();

        if(listener != null) {
            listener.newLocationUploadFinished();
        }
    }


    private float getBatteryLevel() {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if(batteryIntent == null) {
            return -1;
        }
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return -1;
        }

        return ((float) level / (float) scale) * 100.0f;
    }

    @SuppressLint("MissingPermission")
    private int getCellSignalQuality(Context context) {
        int cellQuality = -1;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
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
            if (cellInfoList == null) {
                return cellQuality;
            }
            for (CellInfo cell : cellInfoList) {
                if (cell.isRegistered()) {
                    if (cell instanceof CellInfoLte) {
                        cellQuality = ((CellInfoLte) cell).getCellSignalStrength().getLevel();
                    } else if (cell instanceof CellInfoWcdma) {
                        cellQuality = ((CellInfoWcdma) cell).getCellSignalStrength().getLevel();
                    } else if (cell instanceof CellInfoGsm) {
                        cellQuality = ((CellInfoGsm) cell).getCellSignalStrength().getLevel();
                    } else if (cell instanceof CellInfoCdma) {
                        cellQuality = ((CellInfoCdma) cell).getCellSignalStrength().getLevel();
                    }
                    break;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return cellQuality;

    }
}
