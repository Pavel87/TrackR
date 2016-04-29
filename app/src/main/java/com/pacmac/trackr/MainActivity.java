package com.pacmac.trackr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements NetworkStateListener {


    private TextView tLastLocation, tTimestamp, tAddress;
    private Button mapBtn;
    private ImageButton searchBtn, settingsBtn;
    private ImageView imageBG;

    private boolean haveLocation = false;
    private boolean isConnected = false;

    private AddressResultReceiver resultReceiver;
    private Handler handler;
    SharedPreferences preferences = null;
    private Firebase firebase;
    private LocationRecord locationRecord;

    private NetworkStateChangedReceiver connReceiver = null;


    @Override
    protected void onCreate(Bundle savedInst) {
        super.onCreate(savedInst);
        setContentView(R.layout.activity_main);
        tLastLocation = (TextView) findViewById(R.id.coordinates);
        tTimestamp = (TextView) findViewById(R.id.timestamp);
        tAddress = (TextView) findViewById(R.id.address);

        mapBtn = (Button) findViewById(R.id.showMap);
        searchBtn = (ImageButton) findViewById(R.id.search);
        settingsBtn = (ImageButton) findViewById(R.id.settings);

        imageBG = (ImageView) findViewById(R.id.imgBG);
        imageBG.setImageBitmap(setBitmap());

        handler = new Handler();
        resultReceiver = new AddressResultReceiver(handler);
        connReceiver = new NetworkStateChangedReceiver();
        connReceiver.setConnectionListener(this);


        //restore location on reconfiguration
        if (savedInst != null) {
            locationRecord = new LocationRecord(savedInst.getDouble(Constants.KEY_LATITUDE),
                    savedInst.getDouble(Constants.KEY_LONGITUDE), savedInst.getLong(Constants.KEY_TIMESTAMP));
            tLastLocation.setText(locationRecord.toString());
            tTimestamp.setText(parseDate(locationRecord.getTimestamp()));
            tAddress.setText(savedInst.getString(Constants.KEY_ADDRESS));
        }

        // generate unique IDs on first run
        preferences = getSharedPreferences(Constants.PACKAGE_NAME + Constants.PREF_TRACKR, MODE_PRIVATE);
        if (preferences.getBoolean(Constants.FIRST_RUN, true)){
            SharedPreferences.Editor editor = preferences.edit();
            String uniqueID = generateUniqueID().substring(0,24);
            editor.putString(Constants.TRACKING_ID, uniqueID);
            editor.putString(Constants.RECEIVING_ID, uniqueID);
            editor.putBoolean(Constants.FIRST_RUN, false);
            editor.commit();
        }

        checkConnectivity();

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

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               openSettings();
            }
        });

        Firebase.setAndroidContext(getApplicationContext());
        firebase = new Firebase("https://trackr1.firebaseio.com");
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }


    private Bitmap setBitmap(){

        BitmapDrawable drawable = (BitmapDrawable) imageBG.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        Bitmap blurred = Blurring.getBitmapBlurry(bitmap, 13, getApplicationContext());
        return blurred;
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
            retrieveLocation();
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_connection), Toast.LENGTH_SHORT)
                    .show();
    }

    private void openMap() {
        if (isConnected && haveLocation) {
            Intent intent = new Intent(this, MapDetailActivity.class);
            intent.putExtra(Constants.KEY_LATITUDE, locationRecord.getLatitude());
            intent.putExtra(Constants.KEY_LONGITUDE, locationRecord.getLongitude());
            intent.putExtra(Constants.KEY_TIMESTAMP, parseDate(locationRecord.getTimestamp()));
            intent.putExtra(Constants.KEY_ADDRESS, tAddress.getText().toString());
            startActivity(intent);
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_connection), Toast.LENGTH_SHORT)
                    .show();
    }


    private void getAdress() {
        if (Geocoder.isPresent())
            startIntentService();
        else {
            tAddress.setText(getResources().getString(R.string.not_available));
        }
    }

    private void retrieveLocation() {
        String child = preferences.getString(Constants.RECEIVING_ID, "Error");
        firebase.child(child).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
               // System.out.println(snapshot.getValue());
                if (snapshot.hasChildren()) {

                    double latitude = (double) snapshot.child("latitude").getValue();
                    double longitude = (double) snapshot.child("longitude").getValue();
                    long timeStamp = (long) snapshot.child("timestamp").getValue();

                    locationRecord = new LocationRecord(latitude, longitude, timeStamp);
                    haveLocation = true;

                    String date = parseDate(locationRecord.getTimestamp());
                    tLastLocation.setText(locationRecord.toString());
                    tTimestamp.setText(date);
                    getAdress();
                }
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.i(Constants.TAG, "Update Cancelled" + firebaseError.getMessage());
            }
        });
    }

    private String parseDate(long timestamp) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        String timezone = calendar.getTimeZone().getDisplayName(false, TimeZone.SHORT, Locale.getDefault());

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        return day + " " + month + " " + String.format("%02d", hour) + ":" + String.format("%02d", minute) + " " + timezone;
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
        Log.d(Constants.TAG, "Conn changed: " + isConnected);
        this.isConnected = isConnected;
        if (isConnected && !haveLocation)
            getLastKnownLocation();
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
        haveLocation = false;
        unregisterReceiver(connReceiver);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {

        if(locationRecord !=null) {
            outState.putDouble(Constants.KEY_LATITUDE, locationRecord.getLatitude());
            outState.putDouble(Constants.KEY_LONGITUDE, locationRecord.getLongitude());
            outState.putLong(Constants.KEY_TIMESTAMP, locationRecord.getTimestamp());
            outState.putString(Constants.KEY_ADDRESS, tAddress.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    private String generateUniqueID(){
        return UUID.randomUUID().toString();
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
