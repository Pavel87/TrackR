package com.pacmac.trackr;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;


/**
 * Created by pacmac on 2017-08-23.
 */


public class IntroActivity extends AppCompatActivity {

    private boolean isPermissionEnabled = true;
    private boolean isAppScheduledForStart = false;

    private SharedPreferences preferences;
    private FrameLayout background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        background = (FrameLayout) findViewById(R.id.introImg);
        background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPermissionEnabled) {
                    checkPermission();
                }
            }
        });

        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                MODE_PRIVATE);

        checkPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (requestCode == Utility.MY_PERMISSIONS_REQUEST) {
            isPermissionEnabled = Utility.checkPermission(getApplicationContext(),
                    Constants.LOCATION_PERMISSION);
        }

        startMainActivityWithOffset(1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isPermissionEnabled) {
            startMainActivityWithOffset(2);
        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            isPermissionEnabled = Utility.checkPermission(getApplicationContext(),
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
                }
            }, delay * 1000);
        }
    }
}
