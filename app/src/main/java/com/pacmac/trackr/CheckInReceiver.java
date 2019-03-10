package com.pacmac.trackr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.tutelatechnologies.sdk.framework.TutelaSDKFactory;

public class CheckInReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        LocationUpdate locationUpdate = LocationUpdate.getLocationUpdateInstance(context, null);
        boolean checkInRequested = locationUpdate.getLocation();
        if (checkInRequested) {
            TrackingForegroundServiceController.startForegroundServiceWithNotification(context, -1);
            Utility.showToast(context.getApplicationContext(), context.getResources().getString(R.string.check_in_msg), 0, true);
        } else {
            Utility.showToast(context.getApplicationContext(), "Only ONE check-in per minute is allowed.", 0, true);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                TutelaSDKFactory.getTheSDK().initializeWithApiKey(Constants.REG_KEY, context.getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
