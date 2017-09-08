package com.pacmac.trackr;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a simple helper object used for collecting data in tracked device.
 * Created by pacmac on 2016-11-21.
 */
public class LocationTxObject {

    private long timestamp = 0;
    private double longitude, latitude;
    private Double batteryLevel = -1.0;
    private int cellQuality = -1;

    public LocationTxObject(double latitude, double longitude, long timestamp, Double batteryLevel, int cellQuality) {
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

    public Double getBatteryLevel() {
        return batteryLevel;
    }

    public int getCellQuality() {
        return cellQuality; }


    @Exclude
    public Map<String, Object> createMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("latitude", latitude);
        result.put("longitude", longitude);
//        result.put("timestamp", timestamp);
        result.put("batteryLevel", batteryLevel);
        result.put("cellQuality", cellQuality);
        return result;
    }

}