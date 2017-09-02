package com.pacmac.trackr;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ExpandableListView;

import com.google.android.gms.common.ConnectionResult;


/**
 * Created by pacmac on 2017-08-27.
 */


public final class HelpActivity extends AppCompatActivity {


    private HelpExpandableListAdapter listAdapter;
    private ExpandableListView listView;
    private int expandedGroupId = -1;

    private boolean isPermissionEnabled = false;
    private boolean isGooglePlayAvailable = false;
    private boolean isGooglePlayVersionHigher = false;
    private boolean isLocationingEnabled = false;
    private int[] googleVersion = new int[]{-1, -1};
    private static final int[] GOOGLE_CLIENT_VERSION = new int[]{15, 6};


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activiy_help);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        listView = (ExpandableListView) findViewById(R.id.expandableHelpList);
        listAdapter = new HelpExpandableListAdapter(getApplicationContext());
        listView.setAdapter(listAdapter);


        listView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int i) {
                if (expandedGroupId > -1 && expandedGroupId < listAdapter.getGroupCount()
                        && expandedGroupId != i) {
                    listView.collapseGroup(expandedGroupId);
                }
                expandedGroupId = i;
            }
        });
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


    @Override
    protected void onResume() {
        super.onResume();
        if (isFunctionalityLimited()) {
            if (listAdapter.getGroupCount() < 8) {
                listAdapter.addTroubleshootingRow();
            }
            listAdapter.updateErrors(HelpActivity.this, isPermissionEnabled, isGooglePlayAvailable,
                    isGooglePlayVersionHigher, isLocationingEnabled, googleVersion);
        } else if (listAdapter.getGroupCount() == 8) {
            listAdapter.removeTroubleshootingRow();
        }
    }

    private boolean isFunctionalityLimited() {

        isPermissionEnabled = Utility.checkSelfPermission(getApplicationContext(), Constants.LOCATION_PERMISSION);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (requestCode == Utility.MY_PERMISSIONS_REQUEST) {
            isPermissionEnabled = Utility.checkSelfPermission(getApplicationContext(),
                    Constants.LOCATION_PERMISSION);
        }
        if (isPermissionEnabled) {
            try {
                isFunctionalityLimited();
            } catch (Exception e) {
                e.printStackTrace();
            }
            listAdapter.updateErrors(HelpActivity.this, isPermissionEnabled, isGooglePlayAvailable,
                    isGooglePlayVersionHigher, isLocationingEnabled, googleVersion);
            //invalidate adapter and views
        }
    }
}
