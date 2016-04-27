package com.pacmac.trackr;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by pacmac on 26/04/16.
 */

public class LocationResolver {

    private Location lastLocation = null;
    private Context context = null;

    private double longitude, latitude;

    public LocationResolver(Context context){
        this.context = context;
        //48.424185, -123.356856  fake location
        latitude = 48.424185;
        longitude = -123.356856;
    }





    private String getAdress() throws IOException {

        if(!Geocoder.isPresent()) return "NA";  // return N/A

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(latitude,longitude,1);

        String result = addresses.get(0).getLocality();
        return result;
    }


    public double getLongitude(){
        return longitude;
    }
    public double getLatitude(){
        return latitude;
    }


}
