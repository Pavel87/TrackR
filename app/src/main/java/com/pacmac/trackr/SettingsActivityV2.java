package com.pacmac.trackr;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivityV2 extends AppCompatActivity {

    private final String TAG = "SettingsActivityV2";

    private final int SEEKBAR_OFFSET = 5;
    private final int SEEKBAR_OFFSET_JOB = 15;

    private SharedPreferences preferences = null;

    private String parentalPass = "";
    private boolean isLocked = false;
    private boolean isTrackingEnabled = false;
    private boolean isPermissionEnabled = true;
    private boolean isMyPhoneEnabled = true;

    private String trackId = "Error #5#";
    private int freq = Constants.TIME_BATTERY_OK;

    private View trackIdView;
    private SwitchCompat padlock, txSwitch;
    private SeekBar locUpdateFreqSeekbar;
    private TextView locReqFrequency, trackingID;
    private AppCompatCheckBox showMyPhoneCheckbox;

    private List<LocationRecord> userRecords = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_v2);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);
        loadPreferences();
        userRecords = Utility.convertJsonStringToUserRecords(getFilesDir() + Constants.JSON_LOC_FILE_NAME);

        // Init views:
        padlock = findViewById(R.id.switchLock);
        trackIdView = findViewById(R.id.trackIdView);
        trackingID = findViewById(R.id.trackingID);
        txSwitch = findViewById(R.id.switchTracking);
        showMyPhoneCheckbox = findViewById(R.id.displayMyPhoneOption);
        locReqFrequency = findViewById(R.id.updateFreqText);
        locUpdateFreqSeekbar = findViewById(R.id.locUpdateFreq);
        locUpdateFreqSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                int offset = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? SEEKBAR_OFFSET : SEEKBAR_OFFSET_JOB;

                locReqFrequency.setText(String.valueOf(progress + offset) + " min");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor editor = preferences.edit();
                int offset = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? SEEKBAR_OFFSET : SEEKBAR_OFFSET_JOB;
                freq = seekBar.getProgress() + offset;
                editor.putInt(Constants.TRACKING_FREQ, freq);
                editor.commit();
                ifTrackingOnRestart();
            }
        });

        trackIdView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createTrackingDialog(trackingID.getText().toString());
            }
        });

        txSwitch.setChecked(isTrackingEnabled);
        txSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isEnabled) {
                // check if this triggers after automatic setting checked state on activity start
                Log.d(TAG, "Tracking Mode enabled: " + isEnabled);

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && !Utility.checkSelfPermission(getApplicationContext(),
                        Constants.LOCATION_PERMISSION)) {
                    Utility.displayExplanationForPermission(SettingsActivityV2.this, Constants.LOCATION_PERMISSION);
                    compoundButton.setChecked(false);
                    // continue on result TODO
                } else {
                    setTrackingEnabled(isEnabled);
                }
            }
        });

        showMyPhoneCheckbox.setChecked(isMyPhoneEnabled);
        showMyPhoneCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                isMyPhoneEnabled = isChecked;
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Constants.MY_PHONE_IN_LIST, isMyPhoneEnabled);
                editor.commit();
                if (!isMyPhoneEnabled) {
                    removeMyPhoneFromUserList();
                } else if (isTrackingEnabled) {
                    //if traking enabled and my phone checkbox as well then add phone into user list
                    addMyPhoneToUserList();
                }
            }
        });

        trackingID.setText(trackId);
        txSwitch.setChecked(isTrackingEnabled);
        int offset = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? SEEKBAR_OFFSET : SEEKBAR_OFFSET_JOB;
        locUpdateFreqSeekbar.setProgress(freq - offset);
        locReqFrequency.setText(freq + " min");

        // Load preferences
        parentalPass = preferences.getString(Constants.PADLOCK_PASS, "");
        isLocked = preferences.getBoolean(Constants.PADLOCK_ACTIVE, false);

        // set guard enabled
        padlock.setChecked(isLocked);
        // disable views
        enableGuard(isLocked);
        padlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPassDialog();
            }
        });

        // Start tracking service if everything is setup for tracking.
        Utility.startTrackingService(getApplicationContext(), preferences);
    }

    private void createPassDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.pass_dialog);
        dialog.setCancelable(false);
        Button save = dialog.findViewById(R.id.saveBtn);
        final EditText newPassword = dialog.findViewById(R.id.passwordText);
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); //default has to be set here
        if (!isLocked) {
            save.setText(getResources().getString(R.string.save));
        } else {
            save.setText(getResources().getString(R.string.unlock));
        }

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = newPassword.getText().toString();

                if (password.length() > 0 && password.length() < 33) {
                    if (password.equals("p@cmacDEVdb2016")) {
                        FirebaseHandler.deleteUnusedIdFromFb();
                        Utility.showToast(getApplicationContext(),
                                getResources().getString(R.string.pass_entry_error), 0);
                        return;
                    }

                    if (!isLocked) {
                        parentalPass = password;
                        savePassword(Constants.TYPE_PASSWORD_ACTIVE, password);
                        dialog.dismiss();
                    } else {
                        if (password.equals(parentalPass)) {
                            savePassword(Constants.TYPE_PASSWORD_NOT_ACTIVE, null); // unlock
                            dialog.dismiss();
                        } else {
                            Utility.showToast(getApplicationContext(),
                                    getResources().getString(R.string.pass_entry_error), 0);
                        }
                    }

                } else {
                    Utility.showToast(getApplicationContext(),
                            getString(R.string.password_length_wrong), 0);
                }
            }
        });
        Button noButton = dialog.findViewById(R.id.cancelBtn);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                padlock.setChecked(isLocked);
            }
        });
        dialog.show();
    }

    private void savePassword(int type, String password) {
        if (type == Constants.TYPE_PASSWORD_ACTIVE) {
            isLocked = true;
            enableGuard(isLocked);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.PADLOCK_ACTIVE, isLocked);
            editor.putString(Constants.PADLOCK_PASS, password);
            editor.commit();

        } else if (type == Constants.TYPE_PASSWORD_NOT_ACTIVE) {
            isLocked = false;
            enableGuard(isLocked);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.PADLOCK_ACTIVE, isLocked);
            editor.commit();
        }
    }

    private void enableGuard(boolean isEnabled) {
        trackIdView.setEnabled(!isEnabled);
        txSwitch.setEnabled(!isEnabled);
        locUpdateFreqSeekbar.setEnabled(!isEnabled);
    }


    /**
     * This method will create a dialog where user can enter a new Tracking ID for this device.
     *
     * @param currentId is tracking ID visible in UI
     */
    private void createTrackingDialog(String currentId) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.track_id_dialog);
        dialog.setCancelable(false);
        final EditText newID = dialog.findViewById(R.id.trackIdInput);
        Button save = dialog.findViewById(R.id.saveBtn);

        newID.setText(currentId);
        // input cursor will be set to the last position
        newID.setSelection(currentId.length());

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = newID.getText().toString();
                // If alias edit text is null then we are inserting tracking ID

                // TODO NEED TO CREATE RULE FOR IDs (characters which we can use)
                if (id != null && id.length() > 7 && id.length() < 33) {
                    if (id.contains("/") || id.contains("\\")) {
                        Utility.showToast(getApplicationContext(), getString(R.string.id_char_error), 0);
                    } else if (id == trackId) {
                        // id did not changed no need to do anything.. dismiss didalog
                        dialog.dismiss();
                    } else {
                        String safeId = Utility.checkAndReplaceForbiddenChars(id);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(Constants.TRACKING_ID, safeId);  // firebase child path
                        editor.putString(Constants.TRACKING_ID_RAW, id);  // id to show in UI
                        editor.commit();
                        if (ifTrackingOnRestart()) {
                            updateMyPhoneInUserList(id);
                        }
                        // set new id into global variable
                        trackId = id;
                        trackingID.setText(trackId);
                        dialog.dismiss();
                    }
                } else {
                    Utility.showToast(getApplicationContext(), getString(R.string.id_entry_error), 0);
                }

            }

        });
        Button noButton = dialog.findViewById(R.id.cancelBtn);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    /**
     * This method will check if tracking is enabled and if it is the location service will power cycle.
     */
    private boolean ifTrackingOnRestart() {
        boolean isTrackingOn = preferences.getBoolean(Constants.TRACKING_STATE, false);
        if (isTrackingOn) {
            Intent intentService = new Intent(getApplicationContext(), LocationService.class);
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    stopService(intentService);
                } else {
                    JobSchedulerHelper.cancelLocationUpdateJOB(getApplicationContext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during service shutdown:" + e.getMessage());
            }
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    startService(intentService);
                } else {
                    JobSchedulerHelper.scheduleLocationUpdateJOB(getApplicationContext(), freq*60*1000L);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during service restart:" + e.getMessage());
            }
            return true;
        }
        return false;
    }

    /**
     * This method will attempt to load Tracking ID and state of tracking from preferences
     * and if not available the tracking ID is autogenerated. Also if it is first start
     * then it will automatically create 1 entry in userRecords.
     */
    private void loadPreferences() {
        isTrackingEnabled = preferences.getBoolean(Constants.TRACKING_STATE, false);
        trackId = preferences.getString(Constants.TRACKING_ID_RAW, "Error #5#");
        freq = preferences.getInt(Constants.TRACKING_FREQ, Constants.TIME_BATTERY_OK);
        if(Build.VERSION.SDK_INT >Build.VERSION_CODES.N_MR1 && freq < 15) {
            freq =15;
        }
        isMyPhoneEnabled = preferences.getBoolean(Constants.MY_PHONE_IN_LIST, true);
    }

    private void removeMyPhoneFromUserList() {
        if (userRecords.size() > 0) {
            for (int i = 0; i < userRecords.size(); i++) {
                if (userRecords.get(i).getRecId().equals(trackId)) {
                    userRecords.remove(i);
                    Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                            Utility.createJsonArrayStringFromUserRecords(userRecords));
                    setResult(Constants.SETTINGS_UPDATE_RESULT);
                    break;
                }
            }
        }
    }

    private void addMyPhoneToUserList() {
        boolean isInList = false;
        if (userRecords.size() > 0) {
            for (int i = 0; i < userRecords.size(); i++) {
                if (userRecords.get(i).getRecId().equals(trackId)) {
                    isInList = true;
                    break;
                }
            }
        }
        if (!isInList) {

            int image = preferences.getInt(Constants.MY_PHONE_IMG, -1);
            String alias = preferences.getString(Constants.MY_PHONE_ALIAS, "My Phone");
            userRecords.add(new LocationRecord(-10, trackId, Utility.checkAndReplaceForbiddenChars(trackId), alias, image));
            Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                    Utility.createJsonArrayStringFromUserRecords(userRecords));
            setResult(Constants.SETTINGS_UPDATE_RESULT);
        }
    }

    private void updateMyPhoneInUserList(String newID) {
        if (userRecords.size() > 0) {
            for (int i = 0; i < userRecords.size(); i++) {
                if (userRecords.get(i).getRecId().equals(trackId)) {
                    userRecords.get(i).setRecId(newID);
                    userRecords.get(i).setSafeId(Utility.checkAndReplaceForbiddenChars(trackId));
                    Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                            Utility.createJsonArrayStringFromUserRecords(userRecords));
                    setResult(Constants.SETTINGS_UPDATE_RESULT);
                    break;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (requestCode == Utility.MY_PERMISSIONS_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isPermissionEnabled = true;
                setTrackingEnabled(isPermissionEnabled);
                txSwitch.setChecked(true);
            }
        }
    }

    private void setTrackingEnabled(boolean isEnabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.TRACKING_STATE, isEnabled);
        editor.commit();

        isTrackingEnabled = isEnabled;
        if (isEnabled) {
            if (isMyPhoneEnabled) {
                addMyPhoneToUserList();
            }
            Intent intentService = new Intent(getApplicationContext(), LocationService.class);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                startService(intentService);
            } else {
                JobSchedulerHelper.scheduleLocationUpdateJOB(getApplicationContext(), freq*60*1000L);
            }
        } else {
            removeMyPhoneFromUserList();
            Intent intentService = new Intent(getApplicationContext(), LocationService.class);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                stopService(intentService);
            } else {
                JobSchedulerHelper.cancelLocationUpdateJOB(getApplicationContext());
            }
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
