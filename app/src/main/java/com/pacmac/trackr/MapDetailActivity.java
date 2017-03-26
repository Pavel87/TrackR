package com.pacmac.trackr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pacmac.trackr.mapmarker.IconGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class MapDetailActivity extends FragmentActivity implements OnMapReadyCallback {

    private int position = 0;
    private List<String> alias = new ArrayList<>();
    private HashMap<Integer, LocationRecord> locationRecList = null;
    private GoogleMap mMap;

    private int markerCount = 0;

//    private int[] CHRISTMAS_ICONS = {R.drawable.img1, R.drawable.img2, R.drawable.img3,
//            R.drawable.img4, R.drawable.img5, R.drawable.img6, R.drawable.img7, R.drawable.img8,
//            R.drawable.img9, R.drawable.img10};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_detail);


        locationRecList = Utility.convertJsonStringToLocList(getFilesDir() + Constants.JSON_LOC_FILE_NAME);
        if (locationRecList == null) {
            locationRecList = new HashMap<>();
        }
        Intent intent = getIntent();
        position = intent.getIntExtra(Constants.KEY_POSIION, -1);
        alias = intent.getStringArrayListExtra(Constants.KEY_ALIAS_ARRAY);

        if (alias.size() == 0) {
            alias.add("TrackR");
        }


        markerCount = locationRecList.size();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mapFragment).commit();
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {

        super.onSaveInstanceState(outState, outPersistentState);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (markerCount == 0 || alias.size() == 0) return; // this should not happen

        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < alias.size(); i++) {
            if (locationRecList.containsKey(i)) {
                final LatLng location = new LatLng(locationRecList.get(i).getLatitude(), locationRecList.get(i).getLongitude());
// CHRISTMAS ICONS
//         int imgIndex = random.nextInt(9);
//                MarkerOptions markerOptions = new MarkerOptions().position(location).title(alias.get(i)).snippet(Utility.parseDate(locationRecList.get(i).getTimestamp()))
//                        .icon(BitmapDescriptorFactory.fromResource(CHRISTMAS_ICONS[imgIndex]));

                IconGenerator iconGenerator = new IconGenerator(getApplicationContext());
                iconGenerator.setStyle(i + 3);
                Bitmap bitmapMarker = iconGenerator.makeIcon(alias.get(i) + "\n"
                        + Utility.parseDate(locationRecList.get(i).getTimestamp()));

//                MarkerOptions markerOptions = new MarkerOptions().position(location).title(alias.get(i)).snippet(Utility.parseDate(locationRecList.get(i).getTimestamp()))
//                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker));

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(location)
                        .icon(BitmapDescriptorFactory
                                .fromBitmap(bitmapMarker))
                        .flat(true);

                mMap.addMarker(markerOptions);
            }
        }

        if (locationRecList.containsKey(position)) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationRecList.get(position).getLatitude(),
                    locationRecList.get(position).getLongitude()), 16f));
        } else {
            for (int i = 0; i < alias.size(); i++) {
                if (locationRecList.containsKey(i)) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationRecList.get(i).getLatitude(),
                            locationRecList.get(i).getLongitude()), 16f));
                    break;
                }
            }
        }

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                CircleOptions cOptions = new CircleOptions();
                cOptions.center(marker.getPosition()).fillColor(getResources().getColor(R.color.marker_area))
                        .strokeColor(getResources().getColor(R.color.map_radius)).radius(15).strokeWidth(0.6f).visible(true);
                mMap.addCircle(cOptions);
                return false;
            }
        });
    }
}
