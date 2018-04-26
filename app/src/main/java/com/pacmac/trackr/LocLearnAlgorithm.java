package com.pacmac.trackr;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by pacmac on 2018-02-05.
 */


public class LocLearnAlgorithm {

    private List<Location> nightList = new ArrayList<>();
    private Context context = null;


    public LocLearnAlgorithm(Context context) {
        this.context = context;
    }

    public void newLocation(Location l, long timestamp) {
        if(isNightTime(timestamp)) {
            nightList.add(l);
            // write to file
        }
    }

    private boolean isNightTime(long timestamp) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        return (hour >= Constants.NIGHT_TIME_INTERVAL[0] || hour <= Constants.NIGHT_TIME_INTERVAL[1]);
    }

    public void loadLocationFromNight(Context context) {
        // load List from file
        String csv = Utility.loadCSVFile(context.getFilesDir() + Constants.NIGHT_LOC_FILE_NAME);
        //convert csv to List
        String[] locations = csv.split(";");
        for(String loc : locations) {
            String[] l = loc.split(",");
            Location location = new Location("");
            location.setLatitude(Double.parseDouble(l[0]));
            location.setLongitude(Double.parseDouble(l[1]));
            nightList.add(location);
        }

        // if length == 50 find most common location

    }
}
