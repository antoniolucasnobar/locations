package locations.nobar.br.savelocations;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;
import locations.nobar.br.savelocations.LocationUtil.LocationHelper;
import locations.nobar.br.savelocations.model.Cidade;
import locations.nobar.br.savelocations.model.Estado;

/**
 * An activity that displays a Google map with a marker (pin) to indicate a particular location.
 */
public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    FirebaseFirestore db;
    LocationHelper locationHelper;
    Location mLastLocation;
    //String state = "Distrito Federal";
    //String city = "Brasília";
    String state = "Bahia";
    String city = "Salvador";
    private String group;

    @BindView(R.id.searchValue)
    EditText searchValueField;
    @BindView(R.id.search_options_spinner)
    Spinner searchOptionsSpinner;
    @BindView(R.id.states_spinner)
    Spinner statesSpinner;
    @BindView(R.id.cities_spinner)
    Spinner citiesSpinner;

    private ArrayAdapter<Estado> statesAdapter;
    private ArrayAdapter<Cidade> citiesAdapter;


    private GoogleMap map;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_map);

        ButterKnife.bind(this);

        LinearLayout spinnerLayout = new LinearLayout(this);
        spinnerLayout.setGravity(Gravity.CENTER);
        addContentView(spinnerLayout,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        progressBar = new ProgressBar(this);
        spinnerLayout.addView(progressBar);
        progressBar.setVisibility(View.VISIBLE);

        UserInformation currentUserInformation = UserInstance.getInstance().getCurrentUserInformation();
        if (currentUserInformation != null) {
            group = currentUserInformation.grupo;
        } else {
            Intent intent = getIntent();
            group = intent.getStringExtra("group");
        }
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        loadSearchOptions();

        db = FirebaseFirestore.getInstance();

        locationHelper = LocationHelper.getInstance(this);

        mLastLocation = locationHelper.getLocation();
        double latitude;
        double longitude;
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();

            Address locationAddress = locationHelper.getAddress(latitude, longitude);

            if (locationAddress != null) {
                city = locationAddress.getSubAdminArea();
                state = locationAddress.getAdminArea();
            }
        }
        loadStates();
        //SearchOption selectedItem = (SearchOption) searchOptionsSpinner.getSelectedItem();
        //searchValueField.setHint(selectedItem.screenName);

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_search, menu);
//
//        // Associate searchable configuration with the SearchView
//        // SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
//        // searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//        //searchView.setOnQueryTextListener(this);
//        return super.onCreateOptionsMenu(menu);
//    }

    public void loadStates() {

        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://servicodados.ibge.gov.br/api/v1/localidades/estados/",
                new TextHttpResponseHandler() {

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Toast.makeText(MapActivity.this, "erro ao recuperar estados: " + throwable.getLocalizedMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {

                        Gson gson = new GsonBuilder().create();
                        Estado[] estados = gson.fromJson(responseString, Estado[].class);
                        Arrays.sort(estados);
                        // Create an ArrayAdapter using the string array and a default searchOptionsSpinner layout
                        statesAdapter = new ArrayAdapter(MapActivity.this,
                                android.R.layout.simple_spinner_item, estados);


// Specify the layout to use when the list of choices appears
                        statesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the searchOptionsSpinner
                        statesSpinner.setAdapter(statesAdapter);
                        statesSpinner.setOnItemSelectedListener(new EstadoSelecionado());
                        if (state != null) {
                            int position = statesAdapter.getPosition(new Estado(state));
                            statesSpinner.setSelection(position);
                        }
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    public void loadCities(Estado estado) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://servicodados.ibge.gov.br/api/v1/localidades/estados/" + estado.getId() + "/municipios",

                new TextHttpResponseHandler() {

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Toast.makeText(MapActivity.this, "erro ao recuperar cidades: " + throwable.getLocalizedMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {

                        Gson gson = new GsonBuilder().create();
                        Cidade[] cidades = gson.fromJson(responseString, Cidade[].class);
                        Arrays.sort(cidades);
                        // Create an ArrayAdapter using the string array and a default searchOptionsSpinner layout
                        ArrayAdapter<Cidade> adapter = new ArrayAdapter(MapActivity.this,
                                android.R.layout.simple_spinner_item, cidades);


// Specify the layout to use when the list of choices appears
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the searchOptionsSpinner
                        citiesSpinner.setAdapter(adapter);

                        if (city != null) {
                            int position = adapter.getPosition(new Cidade(city));
                            citiesSpinner.setSelection(position);
                        }
                        state = null;
                        city = null;
                        //int position = adapter.getPosition(new Estado("Bahia"));
                        // citiesSpinner.setSelection(position);
                    }
                });
    }

    class EstadoSelecionado implements AdapterView.OnItemSelectedListener {

        /**
         * <p>Callback method to be invoked when an item in this view has been
         * selected. This callback is invoked only when the newly selected
         * position is different from the previously selected position or if
         * there was no selected item.</p>
         * <p>
         * Impelmenters can call getItemAtPosition(position) if they need to access the
         * data associated with the selected item.
         *
         * @param parent   The AdapterView where the selection happened
         * @param view     The view within the AdapterView that was clicked
         * @param position The position of the view in the adapter
         * @param id       The row id of the item that is selected
         */
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Estado estadoSelecionado = (Estado) parent.getItemAtPosition(position);
            loadCities(estadoSelecionado);
        }

        /**
         * Callback method to be invoked when the selection disappears from this
         * view. The selection can disappear for instance when touch is activated
         * or when the adapter becomes empty.
         *
         * @param parent The AdapterView that now contains no selected item.
         */
        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    class SearchOption {
        public String screenName;
        public String dbFieldName;

        public SearchOption(String screenName, String dbFieldName) {

            this.screenName = screenName;
            this.dbFieldName = dbFieldName;
        }

        public String toString() {
            return screenName;
        }
    }

    private void loadSearchOptions() {

        String[] listNames = getResources().getStringArray(R.array.search_options_array);

        String[] listValues = getResources().getStringArray(R.array.search_options_db);

        ArrayList<SearchOption> options = new ArrayList<>(listNames.length);

        for (int i = 0; i < listNames.length; i++) {
            options.add(new SearchOption(listNames[i], listValues[i]));
        }

// Create an ArrayAdapter using the string array and a default searchOptionsSpinner layout
        ArrayAdapter<SearchOption> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, options);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the searchOptionsSpinner
        searchOptionsSpinner.setAdapter(adapter);
        searchOptionsSpinner.setOnItemSelectedListener(new SearchOptionsListener());

    }

    class SearchOptionsListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            SearchOption selectedItem = (SearchOption) searchOptionsSpinner.getSelectedItem();
            searchValueField.setHint(selectedItem.screenName);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

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
        this.map = googleMap;
    }

    private void loadMapPointers(final GoogleMap googleMap, String searchValue, String searchType) {
        map.clear();
        progressBar.setVisibility(View.VISIBLE);

        Query query = db.collection("groups").document(group).collection("places");

        Estado estadoSelecionado = (Estado) statesSpinner.getSelectedItem();
        Cidade cidadeSelecionada = (Cidade) citiesSpinner.getSelectedItem();

        query = query.whereEqualTo("state", estadoSelecionado.toString()).whereEqualTo("city", cidadeSelecionada.toString());
        if (searchValue != null && !searchValue.trim().isEmpty()) {
            query = query.whereGreaterThanOrEqualTo(searchType, searchValue).
                       whereLessThan(searchType, searchValue + "\uf8ff");
        }

        query.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    public static final String TAG = "MAP_ACT: ";

                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            double latitudeMedia = 0;
                            double longitudeMedia = 0;
                            QuerySnapshot snapshots = task.getResult();
                            int totalItens = 0;
                            for (DocumentSnapshot document : snapshots) {
                                Map<String, Object> data = document.getData();
                                Log.d(TAG, document.getId() + " => " + data);
                                addMarker(googleMap, data);
                                longitudeMedia += Double.parseDouble(data.get("longitude").toString());
                                latitudeMedia += Double.parseDouble(data.get("latitude").toString());
                                totalItens++;
                            }
                            Toast.makeText(MapActivity.this, totalItens + " resultado(s) encontrado(s)", Toast.LENGTH_SHORT).show();
                            if (totalItens > 0) {
                                LatLng place = new LatLng(latitudeMedia / totalItens, longitudeMedia / totalItens);
                                googleMap.moveCamera(CameraUpdateFactory.newLatLng(place));
                                googleMap.animateCamera(CameraUpdateFactory.zoomTo(10), 1000, null);
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                            Toast.makeText(MapActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                        progressBar.setVisibility(View.GONE);
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

    public void searchPlaces(View view) {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null :
                getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        SearchOption selectedItem = (SearchOption) searchOptionsSpinner.getSelectedItem();
        String searchType = selectedItem.dbFieldName;
        String searchValue = this.searchValueField.getText().toString();
        loadMapPointers(map, searchValue, searchType);

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

