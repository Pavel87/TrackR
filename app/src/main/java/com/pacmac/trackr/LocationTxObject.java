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
    private int batteryLevel = -1;
    private int cellQuality = -1;
    private int id = 4;

    /**
     * public constructor used for re-constructing object from firestorm. DO NOT DELETE!
     */
    public LocationTxObject() {
    }

    public LocationTxObject(double latitude, double longitude, long timestamp, int batteryLevel, int cellQuality) {
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

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public int getCellQuality() {
        return cellQuality; }

    public int getId() {
        return id;
    }

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

    @Override
    public String toString() {
        return "Timestamp: " + getTimestamp() + ", Batt Level: " + getBatteryLevel();
    }
}