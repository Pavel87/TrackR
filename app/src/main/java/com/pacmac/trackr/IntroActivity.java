package com.pacmac.trackr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;


/**
 * Created by pacmac on 2017-08-23.
 */


public class IntroActivity extends AppCompatActivity {

    private boolean isPermissionEnabled = true;
    private boolean isGPSUpToDate = true;
    private boolean isAppScheduledForStart = false;

    private FrameLayout background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        background = findViewById(R.id.introImg);
        background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isGPSUpToDate) {
                    if (Utility.checkPlayServices(IntroActivity.this)) {
                        isGPSUpToDate = true;
                        checkPermission();
                    }
                }

                if (!isPermissionEnabled) {
                    checkPermission();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (requestCode == Utility.MY_PERMISSIONS_REQUEST) {
            isPermissionEnabled = Utility.checkSelfPermission(getApplicationContext(),
                    Constants.LOCATION_PERMISSION);
        }
        if (isGPSUpToDate) {
            startMainActivityWithOffset(1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        checkPermission();

        SharedPreferences preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(Constants.LAST_APP_OPEN_TS, System.currentTimeMillis());
        editor.commit();

        if (Utility.checkPlayServices(this)) {
            isGPSUpToDate = true;
        } else {
            isGPSUpToDate = false;
        }
        checkPermission();

        if (isPermissionEnabled && isGPSUpToDate) {
            startMainActivityWithOffset(2);
        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            isPermissionEnabled = Utility.checkSelfPermission(getApplicationContext(),
                    Constants.LOCATION_PERMISSION);
            if (!isPermissionEnabled) {
                Utility.displayExplanationForPermission(this, Constants.LOCATION_PERMISSION);
            }
        }
    }

    private void startMainActivityWithOffset(int delay) {
        if (!isAppScheduledForStart) {
            isAppScheduledForStart = true;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    Intent intent = new Intent(getApplicationContext(), MainActivityV2.class);
                    startActivity(intent);
                    try {
                        ActivityCompat.finishAffinity(IntroActivity.this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, delay * 1000);
        }
    }
}
