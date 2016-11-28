package com.pacmac.trackr;

import android.app.Application;

import com.firebase.client.Firebase;

/**
 * Created by pacmac on 2016-11-27.
 */


public class TrackRApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(getApplicationContext());
        Firebase.getDefaultConfig().setPersistenceEnabled(false);
    }
}
