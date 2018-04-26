package com.pacmac.trackr;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
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
import android.widget.TextView;

public class AddDeviceActivity extends AppCompatActivity {

    private EditText aliasTxt;
    private EditText trackIdTxt;
    private Button saveBtn;
    private ImageView profileImg;

    private TypedArray stockImages;

    private int position;
    private String alias;
    private String id;
    private int img;
    private int type;

    private boolean imgChanged = false;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        stockImages = getResources().obtainTypedArray(R.array.stockImages);

        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                MODE_PRIVATE);

        aliasTxt = (EditText) findViewById(R.id.devNameInput);
        trackIdTxt = (EditText) findViewById(R.id.devIdInput);
        saveBtn = (Button) findViewById(R.id.saveBtn);
        profileImg = (ImageView) findViewById(R.id.profileImg);

        type = getIntent().getIntExtra(Constants.EDIT_USER_TYPE, 0);
        position = getIntent().getIntExtra(Constants.EDIT_USER_POSITION, -1);
        img = getIntent().getIntExtra(Constants.EDIT_USER_IMG, 0);

        // Set image to default if some error happened
        if(img  >= stockImages.length() || img < 0) {
            // if image list was modified then set it to 0
            img = 0;
        }

        profileImg.setImageDrawable(getApplicationContext().getResources().getDrawable(stockImages
                .getResourceId(img, 0)));

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
            if(!id.equals(preferences.getString(Constants.TRACKING_ID_RAW, "not found"))) {
                type = -11;
            } else {
                trackIdTxt.setEnabled(false);
            }
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
                            preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR,
                                    MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt(Constants.MY_PHONE_IMG, img);
                            editor.putString(Constants.MY_PHONE_ALIAS, alias);
                            editor.commit();
                        }
                        finish();
                        return;
                    } else {
                        Utility.showToast(getApplicationContext(), getString(R.string.id_entry_error), 0);
                    }
                } else {
                    Utility.showToast(getApplicationContext(), getString(R.string.alias_required), 0);
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
                img = data.getIntExtra(Constants.EDIT_USER_IMG, 0);
                if(img != -1) {
                    profileImg.setImageDrawable(getApplicationContext().getResources().getDrawable(stockImages
                            .getResourceId(img, 0)));
                }
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
                    Utility.showToast(getApplicationContext(), "This row can be only deleted in Setting screen.", 0);
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

    private void showExitConfirmationDialog(final Activity activity) {

        final Dialog dialog = new Dialog(AddDeviceActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_default);
        TextView title = dialog.findViewById(R.id.title);
        TextView content = dialog.findViewById(R.id.content);
        title.setText(getString(R.string.dialog_title_exit_activity));
        content.setText(getString(R.string.dialog_content_exit_activity));
        dialog.setCancelable(false);

        Button yesButton = dialog.findViewById(R.id.dialogYes);
        yesButton.setText(getString(R.string.dialog_positive_btn_exit_activity));
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

    @Override
    public void onBackPressed() {
        //orig
        int origImg = getIntent().getIntExtra(Constants.EDIT_USER_IMG, 0);
        String origAlias = getIntent().getStringExtra(Constants.EDIT_USER_ALIAS);
        String origId = getIntent().getStringExtra(Constants.EDIT_USER_ID);

        String newAlias = aliasTxt.getText().toString();
        String newId = trackIdTxt.getText().toString();
        
        if (origAlias!= null && origId!= null && origImg == img && origAlias.equals(newAlias) && origId.equals(newId)) {
            this.finish();
        } else if(newAlias.length() == 0 && newId.length() == 0 && origImg == img) {
            this.finish();
        } else {
            //show Dialog
            showExitConfirmationDialog(AddDeviceActivity.this);
        }
    }
}
