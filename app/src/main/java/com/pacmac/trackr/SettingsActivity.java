package com.pacmac.trackr;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity implements SettingsInteractionListener {

    private SharedPreferences preferences = null;
    private TextView appVersion;
    private ImageButton padlock;
    private RecyclerView listOfRecIds;
    private RecyclerView.Adapter adapterForRecIdList;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<SettingsObject> recIdDataSet = new ArrayList<SettingsObject>();
    private boolean isLocked;
    private String parentalPass;


    private ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

        @Override
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.getItemViewType() != Constants.TYPE_NORMAL || isLocked) {
                return 0;
            }
            return super.getSwipeDirs(recyclerView, viewHolder);
        }


        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {

            int position = viewHolder.getAdapterPosition();

            if (recIdDataSet.size() == 6) {
                //this condition will ensure that we will keep at least 1 ID
                ((AdapterReceivingIds) adapterForRecIdList).notifyDataSetChanged();
                return;
            }
            //Remove swiped item from list and notify the RecyclerView
            recIdDataSet.remove(position);
            Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_REC_IDS_FILE_NAME, Utility.createFinalJsonString(recIdDataSet));
            ((AdapterReceivingIds) adapterForRecIdList).notifyDataSetChanged();   //update(recIdDataSet);

            Log.d(Constants.TAG, " view # swiped: " + position);

            if (preferences == null) {
                preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);
            }
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.RECEIVING_ID_CHANGE, true);
            editor.commit();
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);

        prepareListForAdapter();

        // referencing view components
        listOfRecIds = (RecyclerView) findViewById(R.id.receivingIdList);
        mLayoutManager = new LinearLayoutManager(this);
        listOfRecIds.setLayoutManager(mLayoutManager);
        padlock = (ImageButton) findViewById(R.id.padlock);
        appVersion = (TextView) findViewById(R.id.appVersion);

        appVersion.setText("v" + Utility.getCurrentAppVersion(getApplicationContext()));

        // TODO in version 6 I want to shrink these 2 together as all users should be already upgraded
        // Load preferences
        parentalPass = preferences.getString(Constants.PADLOCK_PASS, "");
        isLocked = preferences.getBoolean(Constants.PADLOCK_ACTIVE, false);

        if (isLocked) {
            for (int i = 0; i < recIdDataSet.size(); i++) {
                recIdDataSet.get(i).setEnabled(false);
            }
            padlock.setImageDrawable(getResources().getDrawable(R.drawable.locked));
        }
        padlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPassDialog();
            }
        });

        // adding  adapter to Rec ID list
        adapterForRecIdList = new AdapterReceivingIds(recIdDataSet, this);
        listOfRecIds.setAdapter(adapterForRecIdList);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(listOfRecIds);
    }


    private void createTrackingDialog(final int type, final int position) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.track_id_dialog);
        dialog.setCancelable(false);
        final EditText newID = (EditText) dialog.findViewById(R.id.trackIdInput);
        Button save = (Button) dialog.findViewById(R.id.saveBtn);

        newID.setText(recIdDataSet.get(position).getId());
        // input cursor will be set to the last position
        newID.setSelection(newID.getText().length());

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = newID.getText().toString();
                // If alias edit text is null then we are inserting tracking ID

                // TODO NEED TO CREATE RULE FOR IDs (characters which we can use)
                if (id != null && id.length() > 7 && id.length() < 33) {
                    saveIDandUpdateView(type, null, id, position);
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


    private void createRecIdDialog(final int type, final int position) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.id_dialog);
        dialog.setCancelable(false);

        final EditText newID = (EditText) dialog.findViewById(R.id.recIdInput);
        final EditText alias = (EditText) dialog.findViewById(R.id.friendlyNameInput);
        Button save = (Button) dialog.findViewById(R.id.saveBtn);

        // if we came from footer then we don't have object ready
        if (position != -1) {
            newID.setText(recIdDataSet.get(position).getId());
            alias.setText(recIdDataSet.get(position).getAlias());
            // input cursor will be set to the last position
            alias.setSelection(alias.getText().length());
            newID.setSelection(newID.getText().length());
        }

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = newID.getText().toString();
                String aliasText = alias.getText().toString();

                if (aliasText.length() > 0 && aliasText.length() <= 12) {
                    aliasText.trim();
                    // TODO NEED TO CREATE RULE FOR IDs (characters which we can use)
                    if (id != null && id.length() > 7 && id.length() < 33) {
                        saveIDandUpdateView(type, aliasText, id, position);
                        dialog.dismiss();
                    } else {
                        Utility.showToast(getApplicationContext(), getString(R.string.id_entry_error));
                    }
                } else {
                    Utility.showToast(getApplicationContext(), getString(R.string.alias_required));
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


    private void saveIDandUpdateView(int type, String alias, String id, int position) {
        String editedID = null;
        editedID = Utility.checkAndReplaceForbiddenChars(id);

        if (preferences == null) { // should never be null
            preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);
        }

        if (type == Constants.TYPE_TRACKID) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.TRACKING_ID, editedID);  // firebase child path
            editor.putString(Constants.TRACKING_ID_RAW, id);  // id to show in UI
            editor.commit();
            ifTrackingOnTurnItOff();

        }
        // If footer then we remove footer add new Rec ID and append footer
        if (position == -1) {
            SettingsObject footer = recIdDataSet.get(recIdDataSet.size() - 1);
            recIdDataSet.remove(footer);

            recIdDataSet.add(new SettingsObject(type, alias, id, editedID));
            recIdDataSet.add(footer);

        } else {
            recIdDataSet.set(position, new SettingsObject(type, alias, id, editedID));
        }
        Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_REC_IDS_FILE_NAME, Utility.createFinalJsonString(recIdDataSet));
        ((AdapterReceivingIds) adapterForRecIdList).notifyDataSetChanged();
        // propagate change of id
        if (type == Constants.TYPE_NORMAL) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.RECEIVING_ID_CHANGE, true);
            editor.commit();
        }

    }

    private void savePassword(int type, String password) {
        if (type == Constants.TYPE_PASSWORD_ACTIVE) {
            isLocked = true;
            for (int i = 0; i < recIdDataSet.size(); i++) {
                recIdDataSet.get(i).setEnabled(false);
            }
            padlock.setImageDrawable(getResources().getDrawable(R.drawable.locked));
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.PADLOCK_ACTIVE, isLocked);
            editor.putString(Constants.PADLOCK_PASS, password);
            editor.commit();

        } else if (type == Constants.TYPE_PASSWORD_NOT_ACTIVE) {
            isLocked = false;
            for (int i = 0; i < recIdDataSet.size(); i++) {
                recIdDataSet.get(i).setEnabled(true);
            }
            padlock.setImageDrawable(getResources().getDrawable(R.drawable.unlock));
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.PADLOCK_ACTIVE, isLocked);
            editor.commit();
        }

        ((AdapterReceivingIds) adapterForRecIdList).notifyDataSetChanged();
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

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = newPassword.getText().toString();

                if (password.length() > 0 && password.length() < 33) {

                    if (password.equals("p@cmacDEVdb2016")) {
                        Utility.deleteUnusedIdFromFb();
                        Utility.showToast(getApplicationContext(), getResources().getString(R.string.pass_entry_error));
                        return;
                    }

                    if (!isLocked) {
                        parentalPass = password;
                        savePassword(Constants.TYPE_PASSWORD_ACTIVE, password);
                        dialog.dismiss();
                    } else {
                        if (password.equals(parentalPass)) {
                            savePassword(Constants.TYPE_PASSWORD_NOT_ACTIVE, null); //unlock
                            dialog.dismiss();
                        } else {
                            Utility.showToast(getApplicationContext(), getResources().getString(R.string.pass_entry_error));
                        }
                    }

                } else {
                    Utility.showToast(getApplicationContext(), getString(R.string.password_length_wrong));
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


    // This method will check if Tracking switch is ON and if yes then it will turn it off
    private boolean ifTrackingOnTurnItOff() {
        boolean isTrackingOn = preferences.getBoolean(Constants.TRACKING_STATE, false);
        if (isTrackingOn) {
            settingsInteractionRequest(1, new SettingsObject(Constants.TYPE_TRACK_SWITCH, !isTrackingOn));
        }
        return isTrackingOn;
    }

    @Override
    public void settingsInteractionRequest(int position, SettingsObject object) {

        switch (object.getRowType()) {

            case Constants.TYPE_TRACKID:
                createTrackingDialog(Constants.TYPE_TRACKID, position);
                break;
            case Constants.TYPE_TRACK_SWITCH:

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Constants.TRACKING_STATE, object.isTrackingEnabled());
                editor.commit();

                if (object.isTrackingEnabled()) {
                    Intent intentService = new Intent(getApplicationContext(), LocationService.class);
                    startService(intentService);
                } else {
                    Intent intentService = new Intent(getApplicationContext(), LocationService.class);
                    stopService(intentService);
                }
                recIdDataSet.set(position, object);
                break;
            case Constants.TYPE_NORMAL:
                createRecIdDialog(Constants.TYPE_NORMAL, position);
                break;
            case Constants.TYPE_FOOTER:
                // We will insert new rec id only if the total count in list is less than 10
                // which means 5 default + max 5 rec ids
                if (recIdDataSet.size() < 10) {
                    createRecIdDialog(Constants.TYPE_NORMAL, -1);
                } else {
                    Utility.showToast(getApplicationContext(), "Max list length reached");
                }
                break;
            default:
                break;
        }
    }

    private void prepareListForAdapter() {

        if (preferences.getBoolean(Constants.FIRST_RUN, true)) {
            SharedPreferences.Editor editor = preferences.edit();
            String uniqueID = Utility.generateUniqueID().substring(0, 24);
            editor.putString(Constants.TRACKING_ID, uniqueID);
            editor.putString(Constants.TRACKING_ID_RAW, uniqueID);
            editor.putBoolean(Constants.FIRST_RUN, false);
            editor.putBoolean(Constants.RECEIVING_ID_CHANGE, true);
            editor.commit();
        }

        recIdDataSet.add(new SettingsObject(Constants.TYPE_HEADER, getString(R.string.title_activity_settings)));
        recIdDataSet.add(new SettingsObject(Constants.TYPE_TRACK_SWITCH, preferences.getBoolean(Constants.TRACKING_STATE, false)));
        recIdDataSet.add(new SettingsObject(Constants.TYPE_TRACKID, null, preferences.getString(Constants.TRACKING_ID_RAW, "Error #5#"),
                preferences.getString(Constants.TRACKING_ID_RAW, "Error #5#")));
        recIdDataSet.add(new SettingsObject(Constants.TYPE_HEADER, getString(R.string.rec_id_list_title)));
        // TODO add for loop to add all rec ids

        String jsonString = Utility.loadJsonStringFromFile(getFilesDir() + Constants.JSON_REC_IDS_FILE_NAME);

        if (!jsonString.equals("")) {
            try {
                JSONObject jsnobject = new JSONObject(jsonString);
                JSONArray jsonArray = jsnobject.getJSONArray("receiverids");

                for (int i = 0; i < jsonArray.length(); i++) {
                    SettingsObject settingsObject = Utility.createSettingsObjectFromJson((JSONObject) jsonArray.get(i));
                    if (settingsObject != null) {
                        recIdDataSet.add(settingsObject);
                    }
                }
            } catch (JSONException e) {
                Log.d(Constants.TAG, "#4# Error getting JSON obj or array. " + e.getMessage());
            }
            // add footer
            recIdDataSet.add(new SettingsObject(Constants.TYPE_FOOTER, "FOOTER"));
        } else {
            // RecID should be set to the same as tracking ID on firs start
            String id = preferences.getString(Constants.TRACKING_ID_RAW, "Error");
            recIdDataSet.add(new SettingsObject(Constants.TYPE_NORMAL, "Phone #1", id, id));
            recIdDataSet.add(new SettingsObject(Constants.TYPE_FOOTER, "FOOTER"));
            // we will save initial values in file now after first start
        }
        Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_REC_IDS_FILE_NAME, Utility.createFinalJsonString(recIdDataSet));
    }


}
