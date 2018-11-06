package com.gregrussell.maptest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class LocationUpdate{

    Context mContext;
    private FusedLocationProviderClient locationClient;
    private static LocationCallback locationCallback;
    private static LocationRequest locationRequest;
    private static Location mLastLocation;
    private static Location mLocation;
    static LocCallback locCallback;


    public LocationUpdate(Context mContext, final LocCallback locCallback){

        this.mContext = mContext;
        this.locCallback = locCallback;
        locationClient = LocationServices.getFusedLocationProviderClient(mContext);
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d("getLocationUpdateAsync5","in onLocationResult");
                if (locationResult == null) {
                    Log.d("getLocationUpdateAsync3","location result is null");
                    return;
                }else{
                    Log.d("getLocationUpdateAsync4","location result is not null");
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...

                    Log.d("getLocationUpdateAsync",location.getLatitude() + ", " + location.getLongitude());
                    mLocation = location;
                    locCallback.callback(mLocation);
                    //stopLocationUpdates(locationClient);
                }
            }
        };


    }


    /**
     * Checks if location updates are permitted, then requests location updates in the interval
     * defined by locationRequest
     * @param context Activity context
     * @param mFusedLocationClient FusedLocationProviderClient used to initiate location requests
     */
    private static void startLocationUpdates(Context context, FusedLocationProviderClient mFusedLocationClient) {


        Log.d("getLocationUpdate","startLocation");
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    null /* Looper */);
            Log.d("getLocationUpdate2","getting location");
        }

        WaitInBackground task = new WaitInBackground();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFusedLocationClient);
    }

    /**
     * Stops the receiving of location updates
     * @param mFusedLocationClient FusedLocationProviderClient initiates stoppage of location updates
     */
    private static void stopLocationUpdates(FusedLocationProviderClient mFusedLocationClient) {


        Log.d("getLocationUpdate6","location updates stopped");
        if(mFusedLocationClient != null){
            try{
                final Task<Void> task = mFusedLocationClient.removeLocationUpdates(locationCallback);
                if(task.isSuccessful()){
                    Log.d("getLocationUpdate6","location updates stopped successfully");
                }else{
                    Log.d("getLocationUpdate6","Location updates stopped unsuccessful " + task.toString());
                }
            }catch (SecurityException e){
                Log.d("getLocationUpdate6", "security exception");
            }
        }

    }

    public Location getLocation(){

        startLocationUpdates(mContext,locationClient);

        return getLastKnownLocation(mContext,locationClient);

    }

    public void getCurrentLocation(){

        startLocationUpdates(mContext,locationClient);

    }

    /**
     * AsyncTask to wait 5000 seconds on a background thread. Make sure to run in parallel to other background
     * tasks by using executeOnExecutor() instead of execute()
     */
    private static class WaitInBackground extends AsyncTask<FusedLocationProviderClient,Void,FusedLocationProviderClient>{

        /**
         * Runs a while loop to wait 2500 milliseconds
         * @param params FusedLocationProviderClient located in the params[0] position
         * @return FusedLocationProviderClient that is passed to onPostExecute
         */
        @Override
        protected FusedLocationProviderClient doInBackground(FusedLocationProviderClient...params){

            long time = System.currentTimeMillis();
            long futureTime = time + 5000;
            while(time < futureTime){
                time = System.currentTimeMillis();
            }

            return params[0];
        }

        /**
         * runs stopLocationUpdates() on the UI Thread
         * @param result FusedLocationProviderClient used as a parameter for stopLocationUpdates()
         */
        @Override
        protected void onPostExecute(FusedLocationProviderClient result){
            //stopLocationUpdates(result);
        }
    }

    /**
     * Checks if location permission is enabled, then gets the last known location
     * Updates the static Location homeLocation with new last known location
     * @param context the activity Context
     * @param mFusedLocationClient FusedLocationProviderClient used to get the location
     */
    private static Location getLastKnownLocation(Context context, FusedLocationProviderClient mFusedLocationClient){


        //run the code if location permission is enabled
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener((Activity)context, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Log.d("location5", "logging");
                    if (location != null) {
                        //do location stuff
                        Log.d("location10", location.getLatitude() + ", " + location.getLongitude());
                        mLastLocation = location;
                        locCallback.callback(location);
                    }
                }
            });
        }
        return mLastLocation;
    }
}

