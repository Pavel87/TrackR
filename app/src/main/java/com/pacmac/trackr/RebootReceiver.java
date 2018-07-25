package com.pacmac.trackr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * Created by pacmac on 28/04/16.
 */

public class RebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, context.MODE_PRIVATE);
        Utility.startTrackingService(context, preferences);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            List<LocationRecord> userRecords = Utility.convertJsonStringToUserRecords(context.getFilesDir() + Constants.JSON_LOC_FILE_NAME);
            if (userRecords.size() > 0 && !userRecords.get(0).getRecId().equals("")) {
                Utility.startFetchingService(context);
            }
        }
    }
}