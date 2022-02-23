package com.airmap.airmapsdk.ui.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.ui.views.AirMapMapView;
import com.airmap.airmapsdk.util.AirMapConstants;
import com.airmap.airmapsdk.util.Utils;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.List;

import timber.log.Timber;

public abstract class MyLocationMapActivity extends AppCompatActivity implements PermissionsListener {

    public static final int REQUEST_LOCATION_PERMISSION = 7737;
    public static final int REQUEST_TURN_ON_LOCATION = 8849;

    private PermissionsManager permissionsManager;

    private LocationComponent locationComponent;

    private AirMapMapView.OnMapLoadListener mapLoadListener;

    private boolean hasLoadedMyLocation;
    private boolean isLocationDialogShowing;
    private boolean isMapFailureDialogShowing;

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupMapLoadListener();
    }

    @Override
    public void onRestart() {
        super.onRestart();

        setupMapLoadListener();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (getMapView() != null && mapLoadListener != null) {
            getMapView().removeOnMapLoadListener(mapLoadListener);
        }
    }

    private void setupMapLoadListener() {
        mapLoadListener = new AirMapMapView.OnMapLoadListener() {
            @Override
            public void onMapLoaded() {
                if (hasLoadedMyLocation) {
                    return;
                }

                // use saved location is there is one
                float savedLatitude = PreferenceManager.getDefaultSharedPreferences(MyLocationMapActivity.this)
                        .getFloat(AirMapConstants.LAST_LOCATION_LATITUDE, 0);
                float savedLongitude = PreferenceManager.getDefaultSharedPreferences(MyLocationMapActivity.this)
                        .getFloat(AirMapConstants.LAST_LOCATION_LONGITUDE, 0);
                if (savedLatitude != 0 && savedLongitude != 0) {
                    getMapView().getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(savedLatitude, savedLongitude), 13));
                }

                setupLocationEngine(getMapView().getMap());
            }

            @Override
            public void onMapFailed(AirMapMapView.MapFailure failure) {
                switch (failure) {
                    case INACCURATE_DATE_TIME_FAILURE:
                        // record issue in firebase & logs
                        Analytics.report(new Exception("Mapbox map failed to load due to invalid date/time"));
                        Timber.e("Mapbox map failed to load due to invalid date/time");

                        // ask user to enable "Automatic Date/Time"
                        new AlertDialog.Builder(MyLocationMapActivity.this)
                                .setTitle(R.string.error_loading_map_title)
                                .setMessage(R.string.error_loading_map_message)
                                .setPositiveButton(R.string.error_loading_map_button, (dialog, which) -> {
                                    // open settings and kill this activity
                                    startActivity(new Intent(Settings.ACTION_DATE_SETTINGS));
                                    finish();
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                        break;
                    case NETWORK_CONNECTION_FAILURE:
                        // record issue in firebase & logs
                        String log = Utils.getMapboxLogs();
                        Analytics.report(new Exception("Mapbox map failed to load due to no network connection: " + log));
                        Timber.e("Mapbox map failed to load due to no network connection");

                        // check if dialog is already showing
                        if (isMapFailureDialogShowing) {
                            return;
                        }

                        // ask user to turn on wifi/LTE
                        new AlertDialog.Builder(MyLocationMapActivity.this)
                                .setTitle(R.string.error_loading_map_title)
                                .setMessage(R.string.error_loading_map_network_message)
                                .setPositiveButton(R.string.error_loading_map_network_button, (dialog, which) -> {
                                    isMapFailureDialogShowing = false;
                                    // open settings and kill this activity
                                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                    finish();
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .setOnDismissListener(dialogInterface -> isMapFailureDialogShowing = false)
                                .show();

                        isMapFailureDialogShowing = true;
                        break;
                    case UNKNOWN_FAILURE:
                    default:
                        // record issue in firebase & logs
                        String logs = Utils.getMapboxLogs();
                        Analytics.report(new Exception("Mapbox map failed to load due to unknown reason: " + logs));
                        Timber.e("Mapbox map failed to load due to unknown reason: %s", logs);

                        //TODO: show generic error to user?
                        break;
                }
            }
        };

        if (getMapView() != null) {
            getMapView().addOnMapLoadListener(mapLoadListener);
        }
    }


    @SuppressLint("MissingPermission")
    protected void setupLocationEngine(MapboxMap map) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            locationComponent = map.getLocationComponent();

            // Set options
            LocationComponentOptions options = LocationComponentOptions.builder(this)
                    .elevation(2f)
                    .accuracyAlpha(0f)
                    .enableStaleState(false)
                    .build();

            // Activate with options
            map.getStyle(style -> {
                LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions
                        .builder(MyLocationMapActivity.this, style)
                        .locationComponentOptions(options)
                        .build();
                locationComponent.activateLocationComponent(locationComponentActivationOptions);
            });
            //map.getStyle((style -> locationComponent.activateLocationComponent(this, style, options)));

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);

            // turn on device GPS
            turnOnLocation();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressLint("MissingPermission")
    public void goToLastLocation(boolean force) {
        if (force) {
            hasLoadedMyLocation = false;
        }

        if (locationComponent != null && locationComponent.getLocationEngine() != null) {
            locationComponent.getLocationEngine().getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
                @Override
                public void onSuccess(LocationEngineResult result) {
                    if (result.getLastLocation() != null) {
                        zoomTo(result.getLastLocation(), force);
                    } else if (force) {
                        turnOnLocation();
                    }
                }

                @Override
                public void onFailure(@NonNull Exception exception) {
                    if (locationComponent.getLastKnownLocation() != null) {
                        zoomTo(locationComponent.getLastKnownLocation(), force);
                    }
                    turnOnLocation();
                }
            });
        } else {
            turnOnLocation();
        }
    }

    private void zoomTo(Location location, boolean force) {
        // only zoom to user's location once
        if (!hasLoadedMyLocation || force) {

            // move map camera
            int duration = getMapView().getMap().getCameraPosition().zoom < 10 ? 2500 : 1000;
            getMapView().getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13), duration);

            // stop location requests
            locationComponent.getLocationEngine().removeLocationUpdates(new LocationEngineCallback<LocationEngineResult>() {
                @Override
                public void onSuccess(LocationEngineResult result) {}

                @Override
                public void onFailure(@NonNull Exception exception) {}
            });
            hasLoadedMyLocation = true;

            // save location to prefs
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putFloat(AirMapConstants.LAST_LOCATION_LATITUDE, (float) location.getLatitude())
                    .putFloat(AirMapConstants.LAST_LOCATION_LONGITUDE, (float) location.getLongitude())
                    .apply();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_TURN_ON_LOCATION: {
                // record analytics
                Analytics.logEvent(Analytics.Event.intro, resultCode == Activity.RESULT_OK ? Analytics.Action.success : Analytics.Action.cancelled, Analytics.Label.TURN_ON_LOCATION_DIALOG);

                // if location turned on, go to current location
                if (resultCode == Activity.RESULT_OK) {
                    goToLastLocation(true);
                } else {
                    hasLoadedMyLocation = true;
                }

                isLocationDialogShowing = false;
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
    }

    @Override
    public void onPermissionResult(boolean granted) {
        Analytics.logEvent(Analytics.Event.map, granted ? Analytics.Action.success : Analytics.Action.cancelled, Analytics.Label.LOCATION_PERMISSIONS);

        if (granted) {
            setupLocationEngine(getMapView().getMap());
        }
    }

    /**
     * This turns on Wifi/cell location tracking using Google Play services
     * It shows a dismissible dialog for users that don't have location already enabled
     */
    @SuppressLint("MissingPermission")
    public void turnOnLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(250);
        locationRequest.setPriority(Utils.useGPSForLocation(this) ? LocationRequest.PRIORITY_HIGH_ACCURACY : LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)
                .build();

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this).checkLocationSettings(settingsRequest);
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            goToLastLocation(false);
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                if (isLocationDialogShowing) {
                    return;
                }

                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MyLocationMapActivity.this, REQUEST_TURN_ON_LOCATION);

                    isLocationDialogShowing = true;
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    protected Location getMyLocation() {
        return getMapView() != null && getMapView().getMap() != null ? getMapView().getMap().getLocationComponent().getLastKnownLocation() : null;
    }

    public void onMapViewCreated() {
        setupMapLoadListener();
    }

    protected abstract AirMapMapView getMapView();

    public void setHasLoadedMyLocation(boolean hasLoadedMyLocation) {
        this.hasLoadedMyLocation = hasLoadedMyLocation;
    }
}
