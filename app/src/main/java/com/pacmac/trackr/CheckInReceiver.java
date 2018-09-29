package com.pacmac.trackr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CheckInReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        LocationUpdate locationUpdate = LocationUpdate.getLocationUpdateInstance(context, null);
        boolean checkInRequested = locationUpdate.getLocation();
        if (checkInRequested) {
            TrackingNotification.startNotification(context, -1);
            Utility.showToast(context.getApplicationContext(), context.getResources().getString(R.string.check_in_msg), 0, true);
        } else {
            Utility.showToast(context.getApplicationContext(), "Only ONE check-in per minute is allowed.", 0, true);
        }
    }
}
