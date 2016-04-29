package com.pacmac.trackr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Created by pacmac on 28/04/16.
 */

public class RebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences preferences = context.getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, context.MODE_PRIVATE);
        boolean isTrackingOn = preferences.getBoolean(Constants.TRACKING_STATE, false);

        if (isTrackingOn) {
            Intent intentService = new Intent(context, LocationService.class);
            context.startService(intentService);
        }
    }
}