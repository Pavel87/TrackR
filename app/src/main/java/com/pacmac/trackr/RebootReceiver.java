package com.pacmac.trackr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * Created by pacmac on 28/04/16.
 */

public class RebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences preferences = context.getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, context.MODE_PRIVATE);
        boolean isTrackingOn = preferences.getBoolean(Constants.TRACKING_STATE, false);
        long updateFreq = preferences.getInt(Constants.TRACKING_FREQ, Constants.TIME_BATTERY_OK) * 60 * 1000;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 && updateFreq < 15*60*1000L) {
            updateFreq = 15*60*1000L;
        }

        if (isTrackingOn) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Intent intentService = new Intent(context, LocationService.class);
                context.startService(intentService);
            } else {
                JobSchedulerHelper.scheduleLocationUpdateJOB(context, updateFreq);
            }
        }
    }
}