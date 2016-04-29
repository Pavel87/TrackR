package com.pacmac.trackr;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by tqm837 on 4/27/2016.
 */

public class FetchAddressService extends IntentService {


    protected ResultReceiver receiver;


    private String addressResult;
    private int codeResult;

    public FetchAddressService() {
        super(Constants.PACKAGE_NAME + ".FetchedAddress");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        double latitude = intent.getDoubleExtra(Constants.KEY_LATITUDE, 0);
        double longitude = intent.getDoubleExtra(Constants.KEY_LONGITUDE, 0);
        receiver = intent.getParcelableExtra(Constants.RECEIVER);

        addressResult = getAdress(latitude, longitude);
        deliverResultToReceiver(codeResult, addressResult);
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        receiver.send(resultCode, bundle);
    }

    public String getAdress(double latitude, double longitude) {

        if(longitude == 0) return getResources().getString(R.string.address_loc_error);
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            Log.d(Constants.TAG, "Cannot translate the location: IO Exception");
            codeResult = Constants.ERROR;
            e.printStackTrace();
        }

        if (addresses == null || addresses.isEmpty()) {
            codeResult = Constants.ERROR;
            return getResources().getString(R.string.address_not_found);
        }

        StringBuilder result = new StringBuilder();

        String temp = addresses.get(0).getSubThoroughfare();    // get house number
        result.append(temp != null ? (temp + ", ") : "");
        temp = addresses.get(0).getThoroughfare();              // getStreet
        result.append(temp != null ? (temp + "\n") : "\n");

        temp = addresses.get(0).getLocality();                  // getCity
        result.append(temp != null ? (temp + ", ") : "");
        temp = addresses.get(0).getPostalCode();                // getPostalCode
        result.append(temp != null ? (temp + "\n") : "\n");

        temp = addresses.get(0).getAdminArea();                  // Region/Province
        result.append(temp != null ? (temp + ", ") : "");
        temp = addresses.get(0).getCountryCode();                // get Country Code
        result.append(temp != null ? (temp + "\n") : "\n");


        if (result == null) {
            codeResult = Constants.ERROR;
            return getResources().getString(R.string.address_na);
        }
        codeResult = Constants.SUCCESS;
        return result.toString();
    }

}
