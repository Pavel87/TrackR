package com.pacmac.trackr;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchTracking = null;
    private SharedPreferences preferences = null;
    private LinearLayout txLayout, rxLayout;
    private TextView tTrackingID, tReceivingID;
    private ImageButton padlock;

    private boolean isLocked;
    private String trackID, receiveID, parentalPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);

        switchTracking = (SwitchCompat) findViewById(R.id.switchTracking);
        switchTracking.setChecked(preferences.getBoolean(Constants.TRACKING_STATE, false));

        txLayout = (LinearLayout) findViewById(R.id.txID);
        rxLayout = (LinearLayout) findViewById(R.id.rxID);

        padlock = (ImageButton) findViewById(R.id.padlock);
        isLocked = preferences.getBoolean(Constants.PADLOCK_ACTIVE, false);

        tTrackingID = (TextView) findViewById(R.id.trackingID);
        trackID = preferences.getString(Constants.TRACKING_ID, "Error");
        tTrackingID.setText(trackID);
        tReceivingID = (TextView) findViewById(R.id.receivingID);
        receiveID = preferences.getString(Constants.RECEIVING_ID, "Error");
        tReceivingID.setText(receiveID);

        parentalPass = preferences.getString(Constants.PADLOCK_PASS, "");

        if (isLocked) {
            switchTracking.setEnabled(false);
            txLayout.setEnabled(false);
            rxLayout.setEnabled(false);
            padlock.setImageDrawable(getResources().getDrawable(R.drawable.locked));
        }


        padlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPassDialog();
            }
        });


        switchTracking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Constants.TRACKING_STATE, isChecked);
                editor.commit();

                if (isChecked) {
                    Intent intentService = new Intent(getApplicationContext(), LocationService.class);
                    startService(intentService);
                } else {
                    Intent intentService = new Intent(getApplicationContext(), LocationService.class);
                    stopService(intentService);
                }
            }
        });
        txLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog(Constants.TYPE_TRACKING_ID);
            }
        });

        rxLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog(Constants.TYPE_RECEIVING_ID);
            }
        });
    }

    private void createDialog(final int type) {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.id_dialog);
        dialog.setCancelable(false);

        Button save = (Button) dialog.findViewById(R.id.saveBtn);
        final EditText newID = (EditText) dialog.findViewById(R.id.idText);
        if (type == Constants.TYPE_TRACKING_ID) newID.setText(trackID);
        else if (type == Constants.TYPE_RECEIVING_ID) newID.setText(receiveID);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = newID.getText().toString();

                if (id.length() > 7) {
                    saveIDtoPref(type, id);
                    dialog.dismiss();
                } else {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.id_entry_error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button noButton = (Button) dialog.findViewById(R.id.cancelBtn);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    private void saveIDtoPref(int type, String text) {
        if (type == Constants.TYPE_TRACKING_ID) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.TRACKING_ID, text);
            editor.commit();
            tTrackingID.setText(text);
            trackID = text;
            if (switchTracking.isChecked()) {
                switchTracking.setChecked(false);
            }
        } else if (type == Constants.TYPE_RECEIVING_ID) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.RECEIVING_ID, text);
            editor.commit();
            tReceivingID.setText(text);
            receiveID = text;
        } else if (type == Constants.TYPE_PASSWORD_ACTIVE) {
            isLocked = true;
            switchTracking.setEnabled(false);
            txLayout.setEnabled(false);
            rxLayout.setEnabled(false);
            padlock.setImageDrawable(getResources().getDrawable(R.drawable.locked));

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.PADLOCK_ACTIVE, isLocked);
            editor.putString(Constants.PADLOCK_PASS, text);
            editor.commit();
        } else if (type == Constants.TYPE_PASSWORD_NOT_ACTIVE) {
            isLocked = false;
            switchTracking.setEnabled(true);
            txLayout.setEnabled(true);
            rxLayout.setEnabled(true);
            padlock.setImageDrawable(getResources().getDrawable(R.drawable.unlock));
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.PADLOCK_ACTIVE, isLocked);
            editor.commit();
        }
    }


    private void createPassDialog() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.pass_dialog);
        dialog.setCancelable(false);

        Button save = (Button) dialog.findViewById(R.id.saveBtn);
        final EditText newPassword = (EditText) dialog.findViewById(R.id.passwordText);
        if (!isLocked) {
            save.setText(getResources().getString(R.string.save));
            newPassword.setText(parentalPass);
        } else {
            save.setText(getResources().getString(R.string.unlock));
        }

        CheckBox checkbox = (CheckBox) dialog.findViewById(R.id.checkbox);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    newPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = newPassword.getText().toString();
                if (!isLocked) {
                    parentalPass = password;
                    saveIDtoPref(Constants.TYPE_PASSWORD_ACTIVE, password);
                    dialog.dismiss();
                } else {
                    if (password.equals(parentalPass)) {
                        saveIDtoPref(Constants.TYPE_PASSWORD_NOT_ACTIVE, null); //unlock
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.pass_entry_error), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        Button noButton = (Button) dialog.findViewById(R.id.cancelBtn);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
        Window window = dialog.getWindow(); // make dialog stretched
        window.setLayout(android.support.v7.app.ActionBar.LayoutParams.MATCH_PARENT, android.support.v7.app.ActionBar.LayoutParams.WRAP_CONTENT);
    }


}
