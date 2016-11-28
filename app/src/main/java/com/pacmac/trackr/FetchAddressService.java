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
    private int itemOrder = 0;

    public FetchAddressService() {
        super(Constants.PACKAGE_NAME + ".FetchedAddress");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        double latitude = intent.getDoubleExtra(Constants.KEY_LATITUDE, 0);
        double longitude = intent.getDoubleExtra(Constants.KEY_LONGITUDE, 0);
        receiver = intent.getParcelableExtra(Constants.RECEIVER);
        itemOrder = intent.getIntExtra(Constants.KEY_ITEM_ORDER, 0);


        addressResult = getAdress(latitude, longitude);
        deliverResultToReceiver(codeResult, addressResult);
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        bundle.putInt(Constants.KEY_ITEM_ORDER, itemOrder);
        receiver.send(resultCode, bundle);
    }

    public String getAdress(double latitude, double longitude) {

        if (longitude == 0) return getResources().getString(R.string.address_loc_error);
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 3);
        } catch (IOException e) {
            Log.d(Constants.TAG, "Cannot translate the location: IO Exception");
            codeResult = Constants.ERROR;
            e.printStackTrace();
        }

        // If results available it will iterate through results and fill the fields
        if (addresses != null && addresses.size() > 0) {

            String[] addressOutput = new String[5];

            for (Address address : addresses) {
                if (addressOutput[0] == null) {
                    addressOutput[0] = address.getSubThoroughfare(); // 123 house number
                }
                if (addressOutput[1] == null) {
                    addressOutput[1] = formatStreetString(address.getThoroughfare()); // Street
                }
                if (addressOutput[2] == null) {
                    addressOutput[2] = address.getLocality();  // Vancouver
                }
                if (addressOutput[3] == null) {
                    addressOutput[3] = address.getCountryCode(); // CA
                }

                if (addressOutput[4] == null) {
                    addressOutput[4] = address.getPostalCode();  // 129 321
                }

//                if (addressOutput[5] == null) {
//                    addressOutput[5] = address.getAdminArea(); // British Columbia
//                }

            }

            StringBuilder result = new StringBuilder();
            for (int i = 1; i < addressOutput.length; i++) {
                if (addressOutput[i - 1] == null) {
                    continue;
                }
                result.append(addressOutput[i - 1]).append(", ");
            }
            // add last element
            if (addressOutput[addressOutput.length - 1] != null) {
                result.append(addressOutput[addressOutput.length - 1]);
            }

            // make sure we have some address
            if (addressOutput.length == 0) {
                codeResult = Constants.ERROR;
                return getResources().getString(R.string.address_not_found);
            }

            codeResult = Constants.SUCCESS;
            // remove comma and white space before return
            String output = result.toString().trim();
            if (output.lastIndexOf(",") == output.length() - 1) {
                output = output.substring(0, output.lastIndexOf(","));
            }
            return output;
        }
        codeResult = Constants.ERROR;
        return getResources().getString(R.string.address_not_found);
    }


    private String formatStreetString(String input){

        String formatedInput = input;

        if(input != null){
            if(input.contains("Street")){
                formatedInput = input.replace("Street", "St");
            } else if(input.contains("Avenue")) {
                formatedInput = input.replace("Avenue", "Ave");
            } else if(input.contains("Drive")) {
                formatedInput = input.replace("Avenue", "Dr");
            } else if(input.contains("Place")) {
                formatedInput = input.replace("Avenue", "Pl");
            } else if(input.contains("Highway")) {
                formatedInput = input.replace("Avenue", "Hwy");
            } else if(input.contains("Parkway")) {
                formatedInput = input.replace("Avenue", "Pwy");
            } else {
                formatedInput = input;
            }
        } else {
            return formatedInput;
        }

        return formatedInput;
    }

}
