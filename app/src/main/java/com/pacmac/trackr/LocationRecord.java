package com.pacmac.trackr;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pacmac on 26/04/16.
 */

public class LocationRecord {

    private long timestamp = 0;
    private double longitude, latitude;
    private int id = -1;
    private int profileImageId = 0;
    private double batteryLevel = -1;
    private String address = "";
    private String alias = "TrackR";
    private String recId = "";
    private String safeId = "ID-safe";
    private int cellQuality = -1;
    private List<LocationHistoryRecord> historyRecords = new ArrayList<>();

    public LocationRecord(int id, String recId, String safeId, String alias, int profileImageId) {
        this.id = id;
        this.alias = alias;
        this.recId = recId;
        this.safeId = safeId;
        // -1 is passed for compat reasons now, will use default
        if(profileImageId != -1) {
            this.profileImageId = profileImageId;
        }
    }

    protected void resetParams(){
        timestamp = 0;
        longitude = 0;
        latitude = 0;
        address = "";
        batteryLevel = -1;
    }

    public void updateLocationRecord(double latitude, double longitude, long timestamp, double batteryLevel, int cellQuality, List<LocationHistoryRecord> historyRecords) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.cellQuality = cellQuality;
        this.historyRecords = historyRecords;
    }

//    public void updateLocationRecord(double latitude, double longitude, long timestamp, double batteryLevel, String address) {
//        this.latitude = latitude;
//        this.longitude = longitude;
//        this.timestamp = timestamp;
//        this.batteryLevel = batteryLevel;
//        this.address = address;
//    }


    public LocationRecord(int id, double latitude, double longitude, long timestamp, double batteryLevel, String alias, int cellQuality) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.alias = alias;
        this.cellQuality = cellQuality;
    }

//    public LocationRecord(int id, double latitude, double longitude, long timestamp, double batteryLevel, String address, String alias) {
//        this.id = id;
//        this.latitude = latitude;
//        this.longitude = longitude;
//        this.timestamp = timestamp;
//        this.batteryLevel = batteryLevel;
//        this.address = address;
//        this.alias = alias;
//    }

    public LocationRecord(int id, double latitude, double longitude, long timestamp,
                          double batteryLevel, String address, String alias,
                          String recId, String safeId, int profileImageId, int cellQuality) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.address = address;
        this.alias = alias;
        this.recId = recId;
        this.safeId = safeId;
        this.profileImageId = profileImageId;
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

    public int getProfileImageId() {
        return profileImageId;
    }

    public void setProfileImageId(int profileImageId) {
        this.profileImageId = profileImageId;
    }

    public int getCellQuality() {
        return cellQuality;
    }

    public void setCellQuality(int cellQuality) {
        this.cellQuality = cellQuality;
    }

    public List<LocationHistoryRecord> getHistoryRecords() {
        return historyRecords;
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
                + "\"safeId\": " + "\"" + safeId + "\","
                + "\"profileImageId\": " + "\"" + profileImageId + "\","
                + "\"cellQuality\": " + "\"" + cellQuality + "\"}";
    }

    public String convertToJSONForSettings(int position) {
        return "{ \"alias\": " + "\"" + alias + "\","
                + "\"id\": " + "\"" + id + "\","
                + "\"safeId\": " + "\"" + safeId + "\","
                + "\"position\": " + "\"" + position + "\"}";
    }
}