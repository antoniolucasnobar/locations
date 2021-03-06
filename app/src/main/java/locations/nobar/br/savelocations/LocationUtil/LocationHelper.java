package locations.nobar.br.savelocations.LocationUtil;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import locations.nobar.br.savelocations.IEnderecoCarregado;
import locations.nobar.br.savelocations.SaveLocationActivity;

import static com.google.android.gms.location.LocationSettingsStatusCodes.*;

/**
 * Adaptado por A. Lucas
 */

public class LocationHelper implements PermissionUtils.PermissionResultCallback{


    private static LocationHelper locationHelper;

    public static LocationHelper getInstance(Context context){
        if (locationHelper == null) {
            locationHelper = new LocationHelper(context);
        }
        return locationHelper;
    }

    private Context context;
    private Activity current_activity;

    private boolean isPermissionGranted;

    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 3000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // list of permissions

    private ArrayList<String> permissions=new ArrayList<>();
    private PermissionUtils permissionUtils;

    private final static int PLAY_SERVICES_REQUEST = 1000;
    private final static int REQUEST_CHECK_SETTINGS = 2000;

    private FusedLocationProviderClient mFusedLocationClient;

    public LocationHelper(Context context) {

        this.context=context;
        this.current_activity= (Activity) context;

        permissionUtils=new PermissionUtils(context,this);

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Method to check the availability of location permissions
     * */

    public void checkpermission()
    {
        permissionUtils.check_permission(permissions,"Need GPS permission for getting your location",1);
    }

    private boolean isPermissionGranted() {
        return isPermissionGranted;
    }

    /**
     * Method to verify google play services on the device
     * */

    public boolean checkPlayServices() {

        final GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        final int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                current_activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        googleApiAvailability.getErrorDialog(current_activity,resultCode,
                            PLAY_SERVICES_REQUEST).show();
                    }
                });
            } else {
                showToast("This device is not supported.");
            }
            return false;
        }
        return true;
    }

    int tentativas = 0;

    public void getLocation(final IEnderecoCarregado enderecoCarregado) {
        if (isPermissionGranted()) {
            if (mLastLocation == null || mLastLocation.getAccuracy() > 30) {
                createLocationCallback(enderecoCarregado);
                createLocationRequest();
                startLocationUpdates();
            }

            try {
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(enderecoCarregado.getActivity(), new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                tentativas++;
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    mLastLocation = location;
                                    // Logic to handle location object
                                    atualizarEnderecoEmTela(location, enderecoCarregado);
                                    if (location.getAccuracy() < 30 || tentativas > 8) {
                                        stopLocationUpdates();
                                        tentativas = 0;
                                        showToast("Localização recuperada com sucesso!");
                                    } else {
                                        showToast("Tentando recuperar uma localização mais precisa. Por favor aguarde...");
                                    }
                                }
                            }
                        });

            } catch (SecurityException e) {
                e.printStackTrace();
            }

        }
    }

    private void atualizarEnderecoEmTela(Location location, IEnderecoCarregado enderecoCarregado) {
        mLastLocation = location;
        float accuracy = mLastLocation.getAccuracy();
        Toast.makeText(current_activity, String.format("precisão: %f", accuracy), Toast.LENGTH_SHORT).show();
        enderecoCarregado.onEnderecoCarregado(location);
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        createLocationRequest(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationRequestBaixaPrecisao() {
        createLocationRequest(LocationRequest.PRIORITY_LOW_POWER);
    }


    private void createLocationRequest(int priority) {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(priority);
    }

    /**
     * Creates a callback for receiving location events.
     * @param enderecoCarregado
     */
    private void createLocationCallback(final IEnderecoCarregado enderecoCarregado) {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                tentativas++;

                Location mCurrentLocation = locationResult.getLastLocation();
                if (mLastLocation == null || mCurrentLocation.getAccuracy() < mLastLocation.getAccuracy()) {
                    showToast("Atualizando a localização para uma mais precisa...");
                    //showToast("Data da atualização: " + DateFormat.getTimeInstance().format(new Date()));
                    mLastLocation = mCurrentLocation;
                    enderecoCarregado.onEnderecoCarregado(mLastLocation);
                    if (mLastLocation.getAccuracy() < 30 || tentativas > 8) {
                        stopLocationUpdates();
                    }
                }
            }
        };
    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    public void stopLocationUpdates() {
        if (mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    private LocationCallback mLocationCallback;

            /**
             * Method to display the location on UI
             * */
    public void getLastLocation(final IEnderecoCarregado enderecoCarregado) {
        if (isPermissionGranted()) {

            try
            {
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(current_activity, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    // Logic to handle location object
                                    mLastLocation = location;
                                    float accuracy = mLastLocation.getAccuracy();
                                    enderecoCarregado.onEnderecoCarregado(mLastLocation);
                                }
                            }
                        });
            }
            catch (SecurityException e)
            {
                e.printStackTrace();
            }

        }
    }

    public Address getAddress(double latitude,double longitude)
    {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(context, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude,longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            return addresses.get(0);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public void buildGoogleApi(){
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(current_activity).checkLocationSettings(builder.build());
        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                } catch (final ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            current_activity.runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().

                                                resolvable.startResolutionForResult(
                                                        current_activity,
                                                        REQUEST_CHECK_SETTINGS);

                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                                        }
                                    }

                            );

                            break;
                        case SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });

    }

    /**
     * Method used to connect GoogleApiClient
     */
    public void connectApiClient()
    {
        mGoogleApiClient.connect();
    }

    /**
     * Method used to get the GoogleApiClient
     */
    public GoogleApiClient getGoogleApiCLient()
    {
        return mGoogleApiClient;
    }


    /**
     * Handles the permission results
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
       permissionUtils.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    /**
     * Handles the activity results
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        break;
                    default:
                        break;
                }
                break;
        }
    }


    @Override
    public void PermissionGranted(int request_code) {
        Log.i("PERMISSION","GRANTED");
        isPermissionGranted=true;
    }

    @Override
    public void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions) {
        Log.i("PERMISSION PARTIALLY","GRANTED");
    }

    @Override
    public void PermissionDenied(int request_code) {
        Log.i("PERMISSION","DENIED");
    }

    @Override
    public void NeverAskAgain(int request_code) {
        Log.i("PERMISSION","NEVER ASK AGAIN");
    }



    public void showToast(final String message)
    {
        current_activity.runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              Toast.makeText(current_activity,message, Toast.LENGTH_SHORT).show();
                          }
                      }
        );
    }


}
