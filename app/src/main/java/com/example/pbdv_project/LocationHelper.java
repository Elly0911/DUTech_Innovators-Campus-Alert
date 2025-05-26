package com.example.pbdv_project;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

public class LocationHelper {
    private static final String TAG = "LocationHelper";
    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    public interface MyLocationCallback {
        void onLocationResult(Location location);
        void onLocationError(String error);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void getCurrentLocation(MyLocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            if (context instanceof Activity) {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                callback.onLocationError("Location permission needed");
            }
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            callback.onLocationError("Please enable location services");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(5000)
                .build();

        LocationCallback gmsLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    callback.onLocationError("Unable to get location");
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d(TAG, "Location received: " + location.getLatitude() + "," + location.getLongitude() +
                            " Provider: " + location.getProvider() +
                            " Accuracy: " + location.getAccuracy());
                    fusedLocationClient.removeLocationUpdates(this);
                    callback.onLocationResult(location);
                } else {
                    callback.onLocationError("No location available");
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest,
                gmsLocationCallback, Looper.getMainLooper());
    }

    public Task<Location> getLastLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        return fusedLocationClient.getLastLocation();
    }
}