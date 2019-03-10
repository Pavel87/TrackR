package com.pacmac.trackr;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class ForegroundTrackingService extends Service {
    public ForegroundTrackingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long timestamp;
        if(intent != null) {
            timestamp = intent.getLongExtra(Constants.TIMESTAMP_NOTIFICATION, System.currentTimeMillis());
        } else {
            timestamp =  System.currentTimeMillis();
        }

        startForeground(NOTIFICATION_ID, createNotification(getApplicationContext(), timestamp));
        return START_STICKY_COMPATIBILITY;
    }


    private static final String CHANNEL_ID = "AndroidDeviceTracker";
    private static final int NOTIFICATION_ID = 8888;
    public static final String ADT_CHECKIN_ACTION = "android_device_tracker_checkin";


    public static Notification createNotification(Context context, long timestamp) {
        try {
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
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    .setColorized(true)
                    .setShowWhen(false)
//                    .setColor(context.getResources().getColor(R.color.colorAccent))
                    .addAction(R.drawable.notification_small, "CHECK IN", checkInIntent)
                    .addAction(R.drawable.notification_small, "SETTINGS", settingsIntent)
                    .setOngoing(true);

            if (timestamp > 0) {
                mBuilder.setContentText("Last check-in at " + Utility.parseDate(timestamp));
            } else if (timestamp == -1) {
                mBuilder.setContentText("Processing last checked location");
            }

            return mBuilder.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Android Device TrackeR";
            String description = "Active Tracking1";
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
