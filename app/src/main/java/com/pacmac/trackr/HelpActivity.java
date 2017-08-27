package com.pacmac.trackr;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ExpandableListView;


/**
 * Created by pacmac on 2017-08-27.
 */


public final class HelpActivity extends AppCompatActivity {


    HelpExpandableListAdapter listAdapter;
    ExpandableListView listView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activiy_help);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setTitle("About");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        listView = (ExpandableListView) findViewById(R.id.expandableHelpList);
        listAdapter = new HelpExpandableListAdapter(getApplicationContext());
        listView.setAdapter(listAdapter);
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
