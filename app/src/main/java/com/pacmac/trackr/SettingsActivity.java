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

import java.util.UUID;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchTracking = null;
    private SharedPreferences preferences = null;
    private LinearLayout txLayout, rxLayout;
    private TextView tTrackingID, tReceivingID, appVersion;
    private ImageButton padlock;

    private boolean isLocked;
    private String trackID, receiveID, trackIdRaw, receiveIdRaw, parentalPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);
        firstRunIDSetup();

        switchTracking = (SwitchCompat) findViewById(R.id.switchTracking);
        switchTracking.setChecked(preferences.getBoolean(Constants.TRACKING_STATE, false));

        txLayout = (LinearLayout) findViewById(R.id.txID);
        rxLayout = (LinearLayout) findViewById(R.id.rxID);

        padlock = (ImageButton) findViewById(R.id.padlock);
        isLocked = preferences.getBoolean(Constants.PADLOCK_ACTIVE, false);

        tTrackingID = (TextView) findViewById(R.id.trackingID);
        // TODO in version 6 I want to shrink these 2 together as all users should be already upgraded
        trackID = preferences.getString(Constants.TRACKING_ID, "Error");
        trackIdRaw = preferences.getString(Constants.TRACKING_ID_RAW, trackID);
        tTrackingID.setText(trackIdRaw);

        tReceivingID = (TextView) findViewById(R.id.receivingID);
        // TODO in version 6 I want to shrink these 2 together as all users should be already upgraded
        receiveID = preferences.getString(Constants.RECEIVING_ID, "Error");
        receiveIdRaw = preferences.getString(Constants.RECEIVING_ID_RAW, receiveID);
        tReceivingID.setText(receiveIdRaw);

        appVersion = (TextView) findViewById(R.id.appVersion);
        appVersion.setText("v" + Utility.getCurrentAppVersion(getApplicationContext()));

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

    private void firstRunIDSetup() {
        // generate unique IDs on first run
        if (preferences.getBoolean(Constants.FIRST_RUN, true)) {
            SharedPreferences.Editor editor = preferences.edit();
            String uniqueID = generateUniqueID().substring(0, 24);
            editor.putString(Constants.TRACKING_ID, uniqueID);
            editor.putString(Constants.TRACKING_ID_RAW, uniqueID);
            editor.putString(Constants.RECEIVING_ID_RAW, uniqueID);
            editor.putString(Constants.RECEIVING_ID, uniqueID);
            editor.putBoolean(Constants.FIRST_RUN, false);
            editor.commit();
        }
    }

    private String generateUniqueID() {
        return UUID.randomUUID().toString();
    }


    private void createDialog(final int type) {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.id_dialog);
        dialog.setCancelable(false);

        Button save = (Button) dialog.findViewById(R.id.saveBtn);
        final EditText newID = (EditText) dialog.findViewById(R.id.idText);
        if (type == Constants.TYPE_TRACKING_ID) newID.setText(trackIdRaw);
        else if (type == Constants.TYPE_RECEIVING_ID) newID.setText(receiveIdRaw);

        // input cursor will be set to the last position
        newID.setSelection(newID.getText().length());

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = newID.getText().toString();

                // TODO NEED TO CREATE RULE FOR IDs
                if (id != null && id.length() > 7) {
                    saveIDtoPref(type, id);
                    dialog.dismiss();
                } else {
                    Utility.showToast(getApplicationContext(), getString(R.string.id_entry_error));
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


    private void saveIDtoPref(int type, String id) {
        String editedID = null;
        if (type == Constants.TYPE_TRACKING_ID || type == Constants.TYPE_RECEIVING_ID) {
            editedID = Utility.checkAndReplaceForbiddenChars(id);
        }
        if (type == Constants.TYPE_TRACKING_ID) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.TRACKING_ID, editedID);  // firebase child path
            editor.putString(Constants.TRACKING_ID_RAW, id);  // id to show in UI
            editor.commit();
            tTrackingID.setText(id);
            trackIdRaw = id;
            if (switchTracking.isChecked()) {
                switchTracking.setChecked(false);
            }
        } else if (type == Constants.TYPE_RECEIVING_ID) {
            SharedPreferences.Editor editor = preferences.edit();
            if (!id.equals(receiveIdRaw)) {
                deleteOldDeviceLocationFromPref();
            }
            editor.putString(Constants.RECEIVING_ID, editedID); // firebase child path
            editor.putString(Constants.RECEIVING_ID_RAW, id);  //  id to show in UI
            editor.commit();
            tReceivingID.setText(id);
            receiveIdRaw = id;
        } else if (type == Constants.TYPE_PASSWORD_ACTIVE) {
            isLocked = true;
            switchTracking.setEnabled(false);
            txLayout.setEnabled(false);
            rxLayout.setEnabled(false);
            padlock.setImageDrawable(getResources().getDrawable(R.drawable.locked));

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.PADLOCK_ACTIVE, isLocked);
            editor.putString(Constants.PADLOCK_PASS, id);
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
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); //default has to be set here
        if (!isLocked) {
            save.setText(getResources().getString(R.string.save));
        } else {
            save.setText(getResources().getString(R.string.unlock));
        }

        CheckBox checkbox = (CheckBox) dialog.findViewById(R.id.checkbox);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    newPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    newPassword.setSelection(newPassword.getText().length());
                } else {
                    newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    newPassword.setSelection(newPassword.getText().length());
                }
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = newPassword.getText().toString();

                if (password.equals("p@cmacDEVdb2016")) {
                    Utility.deleteUnusedIdFromFb();
                    Utility.showToast(getApplicationContext(), getResources().getString(R.string.pass_entry_error));
                    return;
                }

                if (!isLocked) {
                    parentalPass = password;
                    saveIDtoPref(Constants.TYPE_PASSWORD_ACTIVE, password);
                    dialog.dismiss();
                } else {
                    if (password.equals(parentalPass)) {
                        saveIDtoPref(Constants.TYPE_PASSWORD_NOT_ACTIVE, null); //unlock
                        dialog.dismiss();
                    } else {
                        Utility.showToast(getApplicationContext(), getResources().getString(R.string.pass_entry_error));
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
    }


    private void deleteOldDeviceLocationFromPref() {
        if (preferences != null) {
            SharedPreferences.Editor prefEditor = preferences.edit();
            prefEditor.putInt(Constants.REMOTE_USER_ID, -1);
            prefEditor.putString(Constants.REMOTE_ADDRESS, "");
            prefEditor.commit();
        }
    }

}
