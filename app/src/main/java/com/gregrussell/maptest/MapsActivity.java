package com.gregrussell.maptest;

import android.Manifest;
import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocCallback {

    private GoogleMap mMap;
    static DataBaseHelperTracker myDBHelper;
    Polyline line;
    Marker myLocationMarker;
    LocationCallback mLocationCallback;
    static FusedLocationProviderClient mFusedLocationClient;
    LocationRequest mLocationRequest;
    TextView textView;
    Location myLocation;
    static Context mContext;

    public static final String START_SERVICE = "start";
    public static final String STOP_SERVICE = "stop";
    public static final String SERVICE_MESSAGE = "message";

    @Override
    protected void onPause(){
        super.onPause();
        Intent serviceIntent = new Intent(this,TrackLocationBackground.class);
        serviceIntent.putExtra(SERVICE_MESSAGE, START_SERVICE);
        startService(serviceIntent);
    }

    @Override
    protected void onResume(){
        super.onResume();
        Intent serviceIntent = new Intent(this,TrackLocationBackground.class);
        serviceIntent.putExtra(SERVICE_MESSAGE, STOP_SERVICE);
        startService(serviceIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mContext = this;
        myDBHelper = new DataBaseHelperTracker(this);
        try{
            myDBHelper.createDataBase();

        }catch (IOException e){
            throw new Error("unable to create db");

        }
        try{
            myDBHelper.openDataBase();
        }catch (SQLException sqle){
            throw sqle;

        }
        //myDBHelper.dropAllTables();
        //myDBHelper.startNewRoute();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                                Log.d("lastLocation","handled");
                                myLocation = location;
                                LatLng myLatLng = new LatLng(myLocation.getLatitude(),myLocation.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng,20));
                                myLocationMarker = mMap.addMarker(new MarkerOptions().
                                        position(myLatLng).icon(BitmapDescriptorFactory.
                                        defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title("My Location"));
                            }
                        }
                    });
        }

        LocationUpdate locationUpdate = new LocationUpdate(this, this);
        myLocation = locationUpdate.getLocation();



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /*mLocationCallback = new LocationCallback(){

            @Override
            public void onLocationResult (LocationResult locationResult){
                if(locationResult == null){
                    return;
                }
                for(Location location : locationResult.getLocations()){

                    Log.d("getLocationUpdate2Map","getting location");
                }
            };
        };
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        startLocationUpdates();*/
    }

    private void startLocationUpdates() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
        }
    }

    @Override
    public void callback(Location location){

        Log.d("locationUpdate","callback");
        List<LatLng> list = myDBHelper.addPoints(myDBHelper.mostRecentRoute(),location);
        if(myLocationMarker != null) {
            myLocationMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }
        Log.d("locationUpdate2","list size: " + list.size());
        if(list!= null && list.size()>0) {
            line = mMap.addPolyline(new PolylineOptions());
            line.setPoints(list);
            Location current = new Location("");

        }

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

        // Add a marker in Sydney and move the camera

        LatLng farthestLocation = new LatLng(38.830833,-77.134722);

        LatLng sydney = new LatLng(-34, 151);


        mMap.addMarker(new MarkerOptions().position(farthestLocation));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
        //zoom levels 5 miles - 13 20miles - 10, 50mils - 8


    }

    public static class TrackLocationBackground extends IntentService {

        public TrackLocationBackground() {
            super("TrackLocationBackground");
        }

        @Override
        protected void onHandleIntent(Intent workIntent) {

            String message = workIntent.getStringExtra(SERVICE_MESSAGE);
            if (message.equals(START_SERVICE)) {
                //start
                Log.d("serviceBackground","Start Service");
                LocationUpdate locationUpdate = new LocationUpdate(mContext, new LocCallback() {
                    @Override
                    public void callback(Location location) {
                        Log.d("serviceBackground","location callback");
                        myDBHelper.addPoints(myDBHelper.mostRecentRoute(),location);
                    }
                });

                locationUpdate.getLocation();

            }else {
                //stop
                Log.d("serviceBackground","Stop Service");
                stopSelf();
            }
        }
    }

}
