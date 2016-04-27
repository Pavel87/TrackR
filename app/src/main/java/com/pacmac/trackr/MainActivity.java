package com.pacmac.trackr;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    //constants
    private final String KEY_LATITUDE = "latitude_key";
    private final String KEY_LONGITUDE = "longitude_key";

    private TextView tLastLocation;
    private Button mapBtn, searchBtn;
    private double longitude, latitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapBtn = (Button) findViewById(R.id.showMap);
        searchBtn = (Button) findViewById(R.id.search);

        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMap();
            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // refresh
                getLastKnownLocation();
            }
        });


    }




    private void getLastKnownLocation(){

        tLastLocation = (TextView) findViewById(R.id.coordinates);

        LocationResolver locationResolver = new LocationResolver(getApplicationContext());
        longitude = locationResolver.getLongitude();
        latitude = locationResolver.getLatitude();


        tLastLocation.setText(latitude + "," + longitude);

    }

    private void openMap() {

        Intent intent = new Intent(this, MapDetailActivity.class);
        intent.putExtra(KEY_LATITUDE, latitude);
        intent.putExtra(KEY_LONGITUDE, longitude);
        startActivity(intent);


    }



}
