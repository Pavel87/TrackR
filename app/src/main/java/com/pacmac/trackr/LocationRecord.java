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
    private String alias = "TrackR";
    private String recId = "";
    private String safeId = "ID-safe";

    public LocationRecord(int id, String recId, String safeId, String alias) {
        this.id = id;
        this.alias = alias;
        this.recId = recId;
        this.safeId = safeId;
    }

    public void updateLocationRecord(double latitude, double longitude, long timestamp, double batteryLevel) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
    }

    public void updateLocationRecord(double latitude, double longitude, long timestamp, double batteryLevel, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.address = address;
    }


    public LocationRecord(int id, double latitude, double longitude, long timestamp, double batteryLevel, String alias) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.alias = alias;
    }

    public LocationRecord(int id, double latitude, double longitude, long timestamp, double batteryLevel, String address, String alias) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.address = address;
        this.alias = alias;
    }

    public LocationRecord(int id, double latitude, double longitude, long timestamp, double batteryLevel, String address, String alias, String recId, String safeId) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.address = address;
        this.alias = alias;
    }

    public LocationRecord() {
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

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setBatteryLevel(double batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getSafeId() {
        return safeId;
    }

    public void setSafeId(String safeId) {
        this.safeId = safeId;
    }

    public String getRecId() {
        return recId;
    }

    public void setRecId(String recId) {
        this.recId = recId;
    }

    @Override
    public String toString() {
        return getLatitude() + ", " + getLongitude();
    }


    public String getJSONString() {
        return "{  \"id\": " + "\"" + id + "\","
                + "\"latitude\": " + "\"" + latitude + "\","
                + "\"longitude\": " + "\"" + longitude + "\","
                + "\"timestamp\": " + "\"" + timestamp + "\","
                + "\"batteryLevel\": " + "\"" + batteryLevel + "\","
                + "\"address\": " + "\"" + address + "\","
                + "\"alias\": " + "\"" + alias + "\","
                + "\"recId\": " + "\"" + recId + "\","
                + "\"safeId\": " + "\"" + safeId + "\"}";
    }

    public String convertToJSONForSettings(int position) {
        return "{ \"alias\": " + "\"" + alias + "\","
                + "\"id\": " + "\"" + id + "\","
                + "\"safeId\": " + "\"" + safeId + "\","
                + "\"position\": " + "\"" + position + "\"}";
    }
}