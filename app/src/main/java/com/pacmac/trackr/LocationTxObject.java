package com.pacmac.trackr;

/**
 * This is a simple helper object used for collecting data in tracked device.
 * Created by pacmac on 2016-11-21.
 */



public class LocationTxObject {

    private long timestamp = 0;
    private double longitude, latitude;
    private double batteryLevel = -1;
    private int cellQuality = -1;

    public LocationTxObject(double latitude, double longitude, long timestamp, double batteryLevel, int cellQuality) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.cellQuality = cellQuality;
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

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public int getCellQuality() {
        return cellQuality; }
}