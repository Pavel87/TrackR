package com.pacmac.trackr;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import com.tutelatechnologies.sdk.framework.TutelaSDKFactory;


/**
 * Created by pacmac on 2016-11-27.
 */
public class TrackRApplication extends Application {

    private static boolean useAltDatabase = true;

    @Override
    public void onCreate() {
        super.onCreate();



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
//                TutelaSDKFactory.getTheSDK().initializeWithApiKey(Constants.REG_KEY, this, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isUseAltDatabase() {
        return useAltDatabase;
    }

    public static void setUseAltDatabase(boolean useAltDatabase) {
        TrackRApplication.useAltDatabase = useAltDatabase;
        Log.d("PACMAC", "USE ALT DB: " + useAltDatabase);
    }
}
