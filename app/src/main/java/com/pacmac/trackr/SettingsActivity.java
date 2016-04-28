package com.pacmac.trackr;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;

public class SettingsActivity extends AppCompatActivity {

    SwitchCompat switchTracking = null;
    SharedPreferences preferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);

        switchTracking = (SwitchCompat) findViewById(R.id.switchTracking);
        switchTracking.setChecked(preferences.getBoolean(Constants.TRACKING_STATE, false));

        switchTracking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Constants.TRACKING_STATE, isChecked);
                editor.commit();

            }
        });

    }
}
