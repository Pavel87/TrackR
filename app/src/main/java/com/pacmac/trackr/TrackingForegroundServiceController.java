package com.pacmac.trackr;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class TrackingForegroundServiceController {

    public static void stopForegroundServiceWithNotification(Context context) {
        Intent intent = new Intent(context, ForegroundTrackingService.class);
        context.stopService(intent);
    }

    public static void startForegroundServiceWithNotification(Context context, long timestamp) {
        Intent intent = new Intent(context, ForegroundTrackingService.class);
        intent.putExtra(Constants.TIMESTAMP_NOTIFICATION, timestamp);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
