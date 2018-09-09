package com.pacmac.trackr;

import android.os.Build;
import android.support.multidex.MultiDexApplication;

import com.tutelatechnologies.sdk.framework.TutelaSDKFactory;


/**
 * Created by pacmac on 2016-11-27.
 */
public class TrackRApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                TutelaSDKFactory.getTheSDK().initializeWithApiKey(Constants.REG_KEY, this, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
