package com.pacmac.trackr;

import java.util.Map;

/**
 * Created by pacmac on 2017-09-11.
 */


public class LocationHistoryRecord {


    private double latitude;
    private double longitude;
    private long timestamp;
    private double batteryLevel;
    private int cellQuality;

    public LocationHistoryRecord(long timestamp, double latitude, double longitude,
                                 double batteryLevel, int cellQuality) {

        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.cellQuality = cellQuality;
    }

    public LocationHistoryRecord(long timestamp, Map<String, Object> historyRecord) {

        this.latitude = (double) historyRecord.get("latitude");
        this.longitude = (double) historyRecord.get("longitude");
        this.timestamp = timestamp;
        this.batteryLevel = Double.parseDouble(String.valueOf(historyRecord.get("batteryLevel")));

        this.cellQuality = ((Long) historyRecord.get("cellQuality")).intValue();
    }


    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public int getCellQuality() {
        return cellQuality;
    }
}
