package com.pacmac.trackr;

import android.content.Context;
import android.location.Location;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;


/**
 * Created by pacmac on 26/04/16.
 */

public class LocationRecord {


    private long timestamp = 0;
    private double longitude, latitude;

    public LocationRecord(double latitude, double longitude, long timestamp) {

        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }


    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return getLatitude() +"\n"+ getLongitude();
    }
}