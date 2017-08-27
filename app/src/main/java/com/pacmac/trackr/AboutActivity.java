package com.pacmac.trackr;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;

/**
 * Created by pacmac on 2017-01-26.
 */


public class AboutActivity extends AppCompatActivity {

    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    // version hardcoded check app gradle file
    // compile 'com.google.android.gms:play-services-maps:9.6.1'
    // compile 'com.google.android.gms:play-services-location:9.6.1'
    private static final int[] GOOGLE_CLIENT_VERSION = new int[]{9, 6};


    private boolean isPermissionEnabled = false;
    private boolean isGooglePlayAvailable = false;
    private boolean isGooglePlayVersionHigher = false;
    private boolean isLocationingEnabled = false;

    private int[] googleVersion = new int[]{-1, -1};

    @Override
    protected void onCreate(Bundle savedInst) {
        super.onCreate(savedInst);
        setContentView(R.layout.about_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setTitle("About");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }


    @Override
    protected void onResume() {
        super.onResume();
        ivalidateViews();
        if (isFunctionalityLimitted()) {
            findViewById(R.id.fixmeLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.fixmeBtn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (((Button) findViewById(R.id.fixmeBtn)).getText().toString().equals("Hide")) {

                        findViewById(R.id.locPermission).setVisibility(View.GONE);
                        findViewById(R.id.locGpsUnavailable).setVisibility(View.GONE);
                        findViewById(R.id.locationDisabled).setVisibility(View.GONE);
                        findViewById(R.id.locGpsVersion).setVisibility(View.GONE);
                        findViewById(R.id.aboutAppText).setVisibility(View.VISIBLE);
                        ((Button) findViewById(R.id.fixmeBtn)).setText("Fix Me");
                    } else {
                        ivalidateViews();
                        ((Button) findViewById(R.id.fixmeBtn)).setText("Hide");
                    }
                }
            });
        }

    }

    private boolean isFunctionalityLimitted() {

        isPermissionEnabled = Utility.checkPermission(getApplicationContext(), LOCATION_PERMISSION);
        // check GooglePlay services
        int googlePlayService = Utility.isGooglePlayAvailable(getApplicationContext());
        if (googlePlayService == ConnectionResult.SUCCESS) {
            isGooglePlayAvailable = true;
            isGooglePlayVersionHigher = true;
        } else {
            googleVersion = Utility.getGooglePlayVersion(getApplicationContext());
            if (googleVersion[0] >= GOOGLE_CLIENT_VERSION[0] && googleVersion[1] >= GOOGLE_CLIENT_VERSION[1]) {
                isGooglePlayVersionHigher = true;
            }
        }

        isLocationingEnabled = Utility.checkIfLocationIsEnabled(getApplicationContext());

        return !isPermissionEnabled || !isGooglePlayAvailable || !isGooglePlayVersionHigher || !isLocationingEnabled;
    }


    private void displayResolutions() {
        if (!isPermissionEnabled) {
            findViewById(R.id.locPermission).setVisibility(View.VISIBLE);
            findViewById(R.id.resolvePermission).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utility.requestPermissions(AboutActivity.this, LOCATION_PERMISSION);
                }
            });

        } else {
            findViewById(R.id.locPermission).setVisibility(View.GONE);
        }
        if (!isGooglePlayVersionHigher) {
            findViewById(R.id.locGpsVersion).setVisibility(View.VISIBLE);
            if (googleVersion[0] != -1) {
                ((TextView) findViewById(R.id.currentGpsVersion)).setText(String.format("Current version: %d.%d", googleVersion[0], googleVersion[1]));
            }
            findViewById(R.id.resolveGooglePlayVersion).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utility.checkPlayServices(AboutActivity.this);
                }
            });
        } else {
            findViewById(R.id.locGpsVersion).setVisibility(View.GONE);
        }
        if (!isGooglePlayAvailable) {
            findViewById(R.id.locGpsUnavailable).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.locGpsUnavailable).setVisibility(View.GONE);
        }
        if (!isLocationingEnabled) {
            findViewById(R.id.locationDisabled).setVisibility(View.VISIBLE);
            findViewById(R.id.resolveLocationDisabled).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openLocationSettings(AboutActivity.this);
                }
            });
        } else {
            findViewById(R.id.locationDisabled).setVisibility(View.GONE);

        }
    }


    private void openLocationSettings(final Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setMessage("Location Settings is DISABLED");
        dialog.setCancelable(false);
        dialog.setPositiveButton("Go To Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(myIntent);
                //get gps
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                paramDialogInterface.dismiss();
            }
        });
        dialog.show();
    }


    private void ivalidateViews() {
        if (findViewById(R.id.fixmeLayout).getVisibility() == View.VISIBLE) {
            if (isFunctionalityLimitted()) {
                findViewById(R.id.aboutAppText).setVisibility(View.GONE);
                ((Button) findViewById(R.id.fixmeBtn)).setText("Hide");
                // show dialog with detail what is wrong
            } else {
                ((Button) findViewById(R.id.fixmeBtn)).setText("Fix me");
                findViewById(R.id.fixmeLayout).setVisibility(View.GONE);
                findViewById(R.id.aboutAppText).setVisibility(View.VISIBLE);
            }
            displayResolutions();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (requestCode == Utility.MY_PERMISSIONS_REQUEST) {
            isPermissionEnabled = Utility.checkPermission(getApplicationContext(),
                    LOCATION_PERMISSION);
        }
        if (isPermissionEnabled) {

            ivalidateViews();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return true;
    }

}
