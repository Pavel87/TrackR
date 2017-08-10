package com.pacmac.trackr;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by pacmac on 2017-08-07.
 */


public final class AddressResolverRunnable implements Runnable {

    private final static String TAG = "AddressResolverRunnable";

    private Context context = null;
    private int rowID = 0;
    private double latitude = 0;
    private double longitude = 0;

    protected AddressResolverRunnable(Context context, int rowID, double latitude, double longitude) {
        this.context = context;
        this.rowID = rowID;
        this.latitude = latitude;
        this.longitude = longitude;
    }


    @Override
    public void run() {
        Log.d(TAG, "Starting address Resolver");
        sendAddressResolverBroadcast(getAddress(latitude,longitude));
    }


    /**
     * Method will use Geocoder and attempts to retrieve the address based on latitude and longitude.
     * @param latitude
     * @param longitude
     * @return Address as String
     */
    public String getAddress(double latitude, double longitude) {
        if (longitude == 0) return context.getResources().getString(R.string.address_loc_error);
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 3);
        } catch (IOException e) {
            Log.e(TAG, "Cannot translate the location: IO Exception: "+e.getMessage());
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
                return context.getResources().getString(R.string.address_not_found);
            }

            // remove comma and white space before return
            String output = result.toString().trim();
            if (output.lastIndexOf(",") == output.length() - 1) {
                output = output.substring(0, output.lastIndexOf(","));
            }
            return output;
        }
        return context.getResources().getString(R.string.address_not_found);
    }


    /**
     * This method is creating shortcuts for certain American road types
     * @param input
     * @return road type shortcut (String)
     */
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


    /**
     * Method will broadcast address back to MainActivity so row views can be updated
     * @param address
     */
    private void sendAddressResolverBroadcast(String address){
        Intent broadcastIntent = new Intent(Constants.ADDRESS_RESOLVER_ACTION);
        broadcastIntent.putExtra(Constants.ADDRESS_RESOLVER_ADDRESS, address);
        broadcastIntent.putExtra(Constants.ADDRESS_RESOLVER_ROWID, rowID);
        Log.d(TAG, "Address resolver: " + address);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
    }

}
