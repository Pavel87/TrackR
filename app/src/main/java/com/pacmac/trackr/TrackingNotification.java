package com.pacmac.trackr;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

public final class TrackingNotification {

    private static final String CHANNEL_ID = "AndroidDeviceTracker";
    private static final int NOTIFICATION_ID = 8888;
    public static final String ADT_CHECKIN_ACTION = "android_device_tracker_checkin";

    public static final void unsubscribeForTrackingNotification(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public static void subscribeForNotification(Context context) {
        createNotificationChannel(context);

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(context, SettingsActivityV2.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent settingsIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Intent checkIntent = new Intent(context.getApplicationContext(), CheckInReceiver.class);
        checkIntent.setAction(ADT_CHECKIN_ACTION);
        PendingIntent checkInIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, checkIntent, 0);


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_small)
                .setContentTitle("Tracking Enabled")
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColorized(true)
                .setColor(context.getResources().getColor(R.color.colorAccent))
                .addAction(R.drawable.notification_small, "Check In", checkInIntent)
                .addAction(R.drawable.notification_small, "Settings", settingsIntent)
                .setOngoing(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());

    }

    private static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Android Device Tracker";
            String description = "Active Tracking";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
