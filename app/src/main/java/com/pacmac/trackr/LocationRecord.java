package com.pacmac.trackr;

/**
 * Created by pacmac on 26/04/16.
 */

public class LocationRecord {

    private long timestamp = 0;
    private double longitude, latitude;
    private int id = -1;
    private double batteryLevel = -1;
    private String address = "";

    public LocationRecord(int id, double latitude, double longitude, long timestamp, double batteryLevel) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
    }

    public LocationRecord(int id, double latitude, double longitude, long timestamp, double batteryLevel, String address) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.address = address;
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

    public String getFormatedTimestamp() {
        return Utility.parseDate(timestamp);
    }

    public int getId() {
        return id;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return getLatitude() +"\n"+ getLongitude();
    }
}