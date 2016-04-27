package com.pacmac.trackr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class MainActivity extends AppCompatActivity implements NetworkStateListener {


    private TextView tLastLocation, tTimestamp, tAddress;
    private Button mapBtn, searchBtn, testBtn;

    private boolean haveLocation = false;
    private boolean isConnected = false;

    private AddressResultReceiver resultReceiver;
    private Handler handler;

    private Firebase firebase;
    private LocationRecord locationRecord;

    private NetworkStateChangedReceiver connReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // locationRecord = new LocationRecord(48.42831778375717, -123.35895001888275, "27.4 14:52",1);

        handler = new Handler();
        resultReceiver = new AddressResultReceiver(handler);
        connReceiver = new NetworkStateChangedReceiver();
        connReceiver.setConnectionListener(this);

        checkConnectivity();

        tLastLocation = (TextView) findViewById(R.id.coordinates);
        tTimestamp = (TextView) findViewById(R.id.timestamp);
        tAddress = (TextView) findViewById(R.id.address);

        mapBtn = (Button) findViewById(R.id.showMap);
        searchBtn = (Button) findViewById(R.id.search);
        testBtn = (Button) findViewById(R.id.test);

        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMap();
            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getLastKnownLocation();
            }
        });

        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // refresh
                firebase.child("1").setValue(locationRecord);
            }
        });

        Firebase.setAndroidContext(getApplicationContext());
        firebase = new Firebase("https://trackr1.firebaseio.com");

    }


    private void checkConnectivity() {
        ConnectivityManager conn = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conn.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            isConnected = true;
        } else {
            isConnected = false;
        }
    }

    private void getLastKnownLocation() {

        if (isConnected) {
            retrieveLocation();  // TODO check for connectivity
            if (!haveLocation) return;

            tLastLocation.setText(locationRecord.toString());
            tTimestamp.setText(locationRecord.getTimestamp());
            getAdress();
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_connection), Toast.LENGTH_SHORT)
            .show();


    }

    private void openMap() {
        if (!haveLocation) return;

        if (isConnected) {
            Intent intent = new Intent(this, MapDetailActivity.class);
            intent.putExtra(Constants.KEY_LATITUDE, locationRecord.getLatitude());
            intent.putExtra(Constants.KEY_LONGITUDE, locationRecord.getLongitude());
            startActivity(intent);
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_connection), Toast.LENGTH_SHORT)
                    .show();

    }


    private void getAdress() {
        if (Geocoder.isPresent()) //return context.getResources().getString(R.string.not_available);
            startIntentService();
        else {
            tAddress.setText(getResources().getString(R.string.not_available));
        }
    }

    private void retrieveLocation() {

        firebase.child("1").addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                System.out.println(snapshot.getValue());
                if (snapshot.hasChildren()) {

                    double latitude = (double) snapshot.child("latitude").getValue();
                    double longitude = (double) snapshot.child("longitude").getValue();
                    String timeStamp = (String) snapshot.child("timestamp").getValue();
                    int timezone = ((Long) snapshot.child("timezone").getValue()).intValue();

                    locationRecord = new LocationRecord(latitude, longitude, timeStamp, timezone);
                    haveLocation = true;
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.i(Constants.TAG, "Update Cancelled" + firebaseError.getMessage());
            }
        });
    }

    private void startIntentService() {
        Intent intent = new Intent(getApplicationContext(), FetchAdressService.class);
        intent.putExtra(Constants.RECEIVER, resultReceiver);
        intent.putExtra(Constants.KEY_LATITUDE, locationRecord.getLatitude());
        intent.putExtra(Constants.KEY_LONGITUDE, locationRecord.getLongitude());
        startService(intent);
    }

    @Override
    public void connectionChanged(boolean isConnected) {
        this.isConnected = isConnected;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(connReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        getLastKnownLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connReceiver);
    }

    /// CLASS TO RESOLVE ADDRESS
    @SuppressLint("ParcelCreator")
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            String address = resultData.getString(Constants.RESULT_DATA_KEY);
            if (resultCode == Constants.SUCCESS) {
                tAddress.setText(address);
            } else {  // GEOCODER returned error
                tAddress.setText(getResources().getString(R.string.not_available));
                Log.e(Constants.TAG, address);
            }
        }
    }


}

//TODO save instance to persist while reconfiguration happens