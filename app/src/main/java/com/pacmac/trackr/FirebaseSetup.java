package com.pacmac.trackr;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public final class FirebaseSetup {


    private static FirebaseOptions db1 = new FirebaseOptions.Builder()
            .setApplicationId("1:420014880695:android:82b90970f95226b2") // Required for Analytics.
            .setApiKey("AIzaSyBnbgddQpJipRY4oHyeNfscKv2FF7KkTVg") // Required for Auth.
            .setDatabaseUrl("https://trackr1.firebaseio.com") // Required for RTDB.
            .setProjectId("firebase-trackr1")
            .setStorageBucket("firebase-trackr1.appspot.com")
            .build();

    private static FirebaseOptions db2 = new FirebaseOptions.Builder()
            .setApplicationId("1:834873097865:android:82b90970f95226b2") // Required for Analytics.
            .setApiKey("AIzaSyCB6WOyqcS3G8hanZdaHpC7JREtDZNXZBM") // Required for Auth.
            .setDatabaseUrl("https://trackr2-backup.firebaseio.com") // Required for RTDB.
            .setProjectId("trackr2-backup")
            .setStorageBucket("trackr2-backup.appspot.com")
            .build();

    private static boolean isPrimaryDBInitialized = false;
    private static boolean isAlternativeDBInitialized = false;

    public static FirebaseDatabase initializeDB(Context context, boolean alternativeDB) {
        FirebaseApp firebaseApp = null;
        if (alternativeDB) {
            if (!isAlternativeDBInitialized) {
                FirebaseApp.initializeApp(context /* Context */, db2, "secondary");
                // Retrieve secondary app.
                isAlternativeDBInitialized = true;
            }
            firebaseApp = FirebaseApp.getInstance("secondary");
        } else {
            if (!isPrimaryDBInitialized) {
                FirebaseApp.initializeApp(context /* Context */, db1, "primary");
                isPrimaryDBInitialized = true;
            }
            // Retrieve secondary app.
            firebaseApp = FirebaseApp.getInstance("primary");
        }
        // Get the database for the other app.
        FirebaseDatabase database = FirebaseDatabase.getInstance(firebaseApp);
        return database;
    }

}
