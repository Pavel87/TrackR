package com.pacmac.trackr;

import android.app.Application;
import android.os.Build;

import com.tutelatechnologies.sdk.framework.TutelaSDKFactory;


/**
 * Created by pacmac on 2016-11-27.
 */
public class TrackRApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                TutelaSDKFactory.getTheSDK().initializeWithApiKey(Constants.REG_KEY, this, true);
//                FirebaseDatabase.getInstance().setLogLevel(Logger.Level.valueOf("DEBUG"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
