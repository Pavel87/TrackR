package com.pacmac.trackr;

/**
 * Created by pacmac on 2016-11-19.
 */


public class SettingsObject {

    // 0 - classic row // -1 == header  // 1 == tracking switch // 2 == tracking ID
    /**
     * safeid is used for matching data in firebase as it ensures correct char set
     */
    private String safeId = "Receiving ID-safe";
    private int rowType = 0; // standard item
    private boolean isTrackingEnabled = false;
    private String alias = "TrackR1";
    private String id = "Receiving ID";
    private boolean isEnabled = true;


    /**
     * Constructor used for tracking ID and receiver IDs
     */
    public SettingsObject(String alias, String id, String safeId) {
        if (alias != null)
            this.alias = alias;
        this.id = id;
        this.safeId = safeId;
        //row type is 0 by default
    }

    /**
     * Constructor can be used for header/footer
     */
    public SettingsObject(int rowType, String id) {
        this.rowType = rowType;
        this.id = id;

    }

    /**
     * Constructor used for track switch item
     */
    public SettingsObject(int rowType, boolean isTrackingEnabled) {
        this.rowType = rowType;
        this.isTrackingEnabled = isTrackingEnabled;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getRowType() {
        return rowType;
    }

    public boolean isTrackingEnabled() {
        return isTrackingEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getSafeId() {
        return safeId;
    }


    public String convertToJSONString(int position) {
        return "{ \"alias\": " + "\"" + alias + "\","
                + "\"id\": " + "\"" + id + "\","
                + "\"safeId\": " + "\"" + safeId + "\","
                + "\"position\": " + "\"" + position + "\"}";
    }
}