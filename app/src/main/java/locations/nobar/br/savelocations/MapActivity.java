package locations.nobar.br.savelocations;

import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import locations.nobar.br.savelocations.LocationUtil.LocationHelper;

/**
 * An activity that displays a Google map with a marker (pin) to indicate a particular location.
 */
public class MapActivity extends AppCompatActivity
            implements OnMapReadyCallback , GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    FirebaseFirestore db;
    LocationHelper locationHelper;
    Location mLastLocation;
    //String state = "Distrito Federal";
    //String city = "Brasília";
    String state = "Bahia";
    String city = "Salvador";
    private String group;

    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Retrieve the content view that renders the map.
            setContentView(R.layout.activity_map);
            Intent intent = getIntent();
            group = intent.getStringExtra("group");
            // Get the SupportMapFragment and request notification
            // when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        db = FirebaseFirestore.getInstance();

        locationHelper=new LocationHelper(this);
        locationHelper.checkpermission();
        // check availability of play services
        if (locationHelper.checkPlayServices()) {

            // Building the GoogleApi client
            locationHelper.buildGoogleApiClient();
        }

        mLastLocation = locationHelper.getLocation();
        double latitude;
        double longitude;
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();

            Address locationAddress = locationHelper.getAddress(latitude, longitude);

            if(locationAddress!=null) {
                city = locationAddress.getLocality();
                state = locationAddress.getAdminArea();

            }
        }

    }

        /**
         * Manipulates the map when it's available.
         * The API invokes this callback when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera. In this case,
         * we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user receives a prompt to install
         * Play services inside the SupportMapFragment. The API invokes this method after the user has
         * installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(final GoogleMap googleMap) {



            CollectionReference states = db.collection("groups").document(group).collection("states");

            CollectionReference collection = states.
                    document(state).collection("cities").document(city).collection("places");
            collection.get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        public static final String TAG = "MAP_ACT: ";

                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (DocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                    addMarker(googleMap, document.getData());
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                           // LatLngBounds mapa = new LatLngBounds(
                             //       new LatLng(-44, 113), new LatLng(-10, 154));
                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(10), 1000, null);

                        }
                    });
        }

    private void addMarker(GoogleMap googleMap, Map<String, Object> data) {
        double latitude = Double.valueOf(data.get("latitude").toString());
        double longitude = Double.valueOf(data.get("longitude").toString());
        String title = data.get("part-name").toString();
        String description = formatDescription(data);

        LatLng place = new LatLng(latitude, longitude);


        MarkerOptions markerOptions = new MarkerOptions().position(place)
                .title(title).snippet(description);
        if (data.containsKey("mandado-sucesso")) {
            Object comSucesso = data.get("mandado-sucesso");
            if ((Boolean) comSucesso){
                markerOptions.icon((BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }
        }

        CustomInfoWindowAdapter customInfoWindow = new CustomInfoWindowAdapter();
        googleMap.setInfoWindowAdapter(customInfoWindow);

        googleMap.addMarker(markerOptions);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(place));

    }

    private String formatDescription(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        if (data.containsKey("process-number")){
            sb.append("Proc.: " + data.get("process-number") + "\n");
        }
        if (data.containsKey("user-name")){
            sb.append("Usuário: " + data.get("user-name") + "\n");
        }
        if (data.containsKey("mandado-sucesso")) {
            Object comSucesso = data.get("mandado-sucesso");
            sb.append("Mandado cumprido " + ((Boolean) comSucesso ? "com " : "sem ") + "sucesso" + "\n");
        }
        if (data.containsKey("timestamp")){
            Date d = (Date) data.get("timestamp");
            if (d != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                sb.append("Data: " + dateFormat.format(d) + "\n");
            }
        }
        if (data.containsKey("description")){
            sb.append("Obs.: " + data.get("description"));
        }

        return sb.toString();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i("Connection failed:", " ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        mLastLocation=locationHelper.getLocation();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        locationHelper.connectApiClient();
    }


    /** Demonstrates customizing the info window and/or its contents. */
    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        // These are both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".

        private final View mContents;

        CustomInfoWindowAdapter() {
            mContents = getLayoutInflater().inflate(R.layout.map_custom_infowindow, null);
        }
        @Override
        public View getInfoWindow(Marker marker) {
            // This means that getInfoContents will be called.
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            TextView name_tv = mContents.findViewById(R.id.name);
            TextView details_tv = mContents.findViewById(R.id.details);
            name_tv.setText(marker.getTitle());
            details_tv.setText(marker.getSnippet());

            return mContents;
        }
    }
}

