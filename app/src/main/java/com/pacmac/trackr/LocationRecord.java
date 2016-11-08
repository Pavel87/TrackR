package com.pacmac.trackr;

/**
 * Created by pacmac on 26/04/16.
 */

public class LocationRecord {

    private long timestamp = 0;
    private double longitude, latitude;
    private int id = -1;
    private float batteryLevel = -1;

    public LocationRecord(int id, double latitude, double longitude, long timestamp, float batteryLevel) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
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

    public int getId() {
        return id;
    }

    public float getBatteryLevel() {
        return batteryLevel;
    }

    @Override
    public String toString() {
        return getLatitude() +"\n"+ getLongitude();
    }
}