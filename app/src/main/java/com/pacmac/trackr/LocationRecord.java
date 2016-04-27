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


    private String timestamp = null;
    private int timezone;
    private double longitude, latitude;

    public LocationRecord(double latitude, double longitude, String timestamp, int timezone) {

        //48.424185, -123.356856  fake location
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
        this.timestamp = timestamp;
    }


    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getTimezone() {
        return timezone;
    }

    @Override
    public String toString() {
        return getLatitude() +"\n"+ getLongitude();
    }
}