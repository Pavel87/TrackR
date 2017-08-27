package com.pacmac.trackr;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class AddDeviceActivity extends AppCompatActivity {

    private EditText aliasTxt;
    private EditText trackIdTxt;
    private Button saveBtn;
    private ImageView profileImg;

    private int position;
    private String alias;
    private String id;
    private int img;
    private int type;

    private boolean imgChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        aliasTxt = (EditText) findViewById(R.id.devNameInput);
        trackIdTxt = (EditText) findViewById(R.id.devIdInput);
        saveBtn = (Button) findViewById(R.id.saveBtn);
        profileImg = (ImageView) findViewById(R.id.profileImg);

        type = getIntent().getIntExtra(Constants.EDIT_USER_TYPE, 0);
        position = getIntent().getIntExtra(Constants.EDIT_USER_POSITION, -1);
        img = getIntent().getIntExtra(Constants.EDIT_USER_IMG, R.drawable.user0);
        profileImg.setImageDrawable(getDrawable(img));

        if (position != -1) {
            alias = getIntent().getStringExtra(Constants.EDIT_USER_ALIAS);
            id = getIntent().getStringExtra(Constants.EDIT_USER_ID);

            aliasTxt.setText(alias);
            trackIdTxt.setText(id);

            aliasTxt.setSelection(alias.length());
            trackIdTxt.setSelection(id.length());
        }

        // -10 means My Phone record only Alias and Img can be changed here
        if (type == -10) {
            trackIdTxt.setEnabled(false);
        }


        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alias = aliasTxt.getText().toString();
                id = trackIdTxt.getText().toString();
                if (alias.length() > 0 && alias.length() <= 12) {
                    alias.trim();
                    // TODO NEED TO CREATE RULE FOR IDs (characters which can be used)
                    if (id != null && id.length() > 7 && id.length() < 33) {

                        //saveIDandUpdateView(type, aliasText, id, position);

                        // if there was no change then send RESULT_CANCELED
                        if (getIntent().getStringExtra(Constants.EDIT_USER_ALIAS) != null
                                && getIntent().getStringExtra(Constants.EDIT_USER_ID) != null) {

                            if (getIntent().getStringExtra(Constants.EDIT_USER_ALIAS).equals(alias)
                                    && getIntent().getStringExtra(Constants.EDIT_USER_ID).equals(id)
                                    && !imgChanged) {
                                setResult(RESULT_CANCELED);
                                finish();
                                return;
                            }

                        }
                        Intent intent = getIntent();
                        intent.putExtra(Constants.EDIT_USER_IMG, img);
                        intent.putExtra(Constants.EDIT_USER_ALIAS, alias);
                        intent.putExtra(Constants.EDIT_USER_ID, id);
                        setResult(Activity.RESULT_OK, intent);

                        // if default my phone alias or img is changed then stored this in preference
                        if(type == -10) {
                            SharedPreferences preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                                    MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt(Constants.MY_PHONE_IMG, img);
                            editor.putString(Constants.MY_PHONE_ALIAS, alias);
                            editor.commit();
                        }
                        finish();
                        return;
                    } else {
                        Utility.showToast(getApplicationContext(), getString(R.string.id_entry_error));
                    }
                } else {
                    Utility.showToast(getApplicationContext(), getString(R.string.alias_required));
                }
            }
        });

        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ImageGallery.class);
                intent.putExtra(Constants.EDIT_USER_IMG, img);
                startActivityForResult(intent, Constants.EDIT_IMAGE_REQUEST_CODE);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Constants.EDIT_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                imgChanged = true;
                img = data.getIntExtra(Constants.EDIT_USER_IMG, R.drawable.user1);
                profileImg.setImageDrawable(getDrawable(img));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_delete:
                if (position == -1) {
                    finish();
                    return true;
                } else if (type == -10) {
                    Utility.showToast(getApplicationContext(), "This row can be only deleted in Setting screen.");
                    return true;
                }
                showDeleteConfirmationDialog(AddDeviceActivity.this);
                break;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return true;
    }

    private void showDeleteConfirmationDialog(final Activity activity) {

        final Dialog dialog = new Dialog(AddDeviceActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_default);
        dialog.setCancelable(false);

        Button yesButton = dialog.findViewById(R.id.dialogYes);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                activity.setResult(Constants.EDIT_DELETE_POSITION, activity.getIntent());
                dialog.dismiss();
                activity.finish();
            }
        });

        Button noButton = dialog.findViewById(R.id.dialogCancel);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

}
