package locations.nobar.br.savelocations;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import locations.nobar.br.savelocations.LocationUtil.LocationHelper;

public class SaveLocationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "SAVE_LOCATIONS - ";
    @BindView(R.id.btnLocation)Button btnProceed;
    @BindView(R.id.tvAddress)TextView tvAddress;
    @BindView(R.id.tvEmpty)TextView tvEmpty;
    @BindView(R.id.partName)EditText partName;
    @BindView(R.id.userName)EditText userName;
    @BindView(R.id.descriptionText)EditText descriptionText;
    @BindView(R.id.mandadoComsucesso) Switch mandadoComsucesso;
    @BindView(R.id.processNumber)EditText processNumber;
    @BindView(R.id.loggedUserInfo)TextView loggedUserInfo;
    @BindView(R.id.loggedUserGroup)TextView loggedUserGroup;

    private Location mLastLocation;
    Address locationAddress;


    double latitude;
    double longitude;

    LocationHelper locationHelper;

    ProgressBar progressBar;
    FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    FirebaseUser currentUser;
    private UserInformation currentUserInformation;
    private String group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_location);

        group = "public";

        locationHelper=new LocationHelper(this);
        locationHelper.checkpermission();

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100,100);
        params.gravity = Gravity.CENTER;
        LinearLayout layout = findViewById(R.id.linearLayoutPrincipal);
        layout.addView(progressBar,params);
        progressBar.setVisibility(View.GONE);

        ButterKnife.bind(this);
        // check availability of play services
        if (locationHelper.checkPlayServices()) {

            // Building the GoogleApi client
            locationHelper.buildGoogleApiClient();
        }
        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null ){
            if (currentUser.isEmailVerified()){
                carregarInformacoesUsuario(currentUser.getUid());
                userName.setVisibility(View.GONE);//o nome virá do cadastro do usuário
            } else {
                showToast("Você deve verificar seu email antes de usar o sistema com login e senha. Por favor, verifique sua caixa de emails. Saindo...");
                firebaseAuth.signOut();
            }
        }
        loggedUserGroup.setText("Grupo: " + group);

        btnProceed.setEnabled(false);

        tvEmpty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadLocation();
            }
        });

        tvAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvAddress.setText("");
                tvAddress.setVisibility(View.GONE);
            }
        });


    }

    private void carregarInformacoesUsuario(String uid) {
        Task<DocumentSnapshot> snapshotTask = db.collection("usersInformation").document(uid).get();
        snapshotTask.addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()){
                    currentUserInformation = task.getResult().toObject(UserInformation.class);
                    group = currentUserInformation.grupo;
                    loggedUserInfo.setText("Usuário: " + currentUserInformation.nome);
                    loggedUserInfo.setVisibility(View.VISIBLE);
                    loggedUserGroup.setText("Group: " + group);
                }
            }
        });
    }

    // Make sure this is the method with just `Bundle` as the signature
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }



    public void saveLocation(View view) {

        showToast("Salvando Lugar...");
        progressBar.setVisibility(View.VISIBLE);  //To show ProgressBar

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        // Create a place
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", mLastLocation.getLatitude());
        location.put("longitude", mLastLocation.getLongitude());
        location.put("part-name", partName.getText().toString());
        location.put("user-name", userName.getText().toString());
        location.put("process-number", processNumber.getText().toString());
        location.put("mandado-sucesso", mandadoComsucesso.isChecked());
        location.put("description", descriptionText.getText().toString());
        location.put("postal-code", locationAddress.getPostalCode());
        location.put("timestamp", FieldValue.serverTimestamp());
        if (currentUser != null ) {
            location.put("email", currentUser.getEmail());
            location.put("verified", currentUser.isEmailVerified());
            location.put("user-name", currentUserInformation.nome);
            location.put("uid", currentUser.getUid());

        }


        String city = locationAddress.getLocality();
        String state = locationAddress.getAdminArea();

        CollectionReference states = db.collection("groups").document(group).collection("states");

// Add a new document with a generated ID
        states.document(state).collection("cities").document(city).collection("places")
                .add(location)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        showToast("Lugar salvo com sucesso: " + locationAddress.getAddressLine(0));
                        progressBar.setVisibility(View.GONE);     // To Hide ProgressBar
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                        showToast("Erro ao salvar: " + e.getLocalizedMessage());
                        progressBar.setVisibility(View.GONE);     // To Hide ProgressBar
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                });
    }


    public void goToMap(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("group", group);
        startActivity(intent);
    }
    public void goToLogin(View view) {
        //Intent intent = new Intent(this, LoginActivity.class);
        Intent intent = new Intent(this, RegisterLoginActivity.class);
        startActivity(intent);
    }
    public void loadLocation() {

        mLastLocation=locationHelper.getLocation();

        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            getAddress();

        } else {

            if(btnProceed.isEnabled())
                btnProceed.setEnabled(false);

            showToast("Couldn't get the location. Make sure location is enabled on the device");
        }
    }

    public void getAddress()
    {

        locationAddress=locationHelper.getAddress(latitude,longitude);

        if(locationAddress!=null)
        {

            String address = locationAddress.getAddressLine(0);
            String address1 = locationAddress.getAddressLine(1);
            String city = locationAddress.getLocality();
            String state = locationAddress.getAdminArea();
            String country = locationAddress.getCountryName();
            String postalCode = locationAddress.getPostalCode();


            String currentLocation;

            if(!TextUtils.isEmpty(address))
            {
                currentLocation=address;

                if (!TextUtils.isEmpty(address1))
                    currentLocation+="\n"+address1;

                if (!TextUtils.isEmpty(city))
                {
                    currentLocation+="\n"+city;

                    if (!TextUtils.isEmpty(postalCode))
                        currentLocation+=" - "+postalCode;
                }
                else
                {
                    if (!TextUtils.isEmpty(postalCode))
                        currentLocation+="\n"+postalCode;
                }

                if (!TextUtils.isEmpty(state))
                    currentLocation+="\n"+state;

                if (!TextUtils.isEmpty(country))
                    currentLocation+="\n"+country;

                tvEmpty.setVisibility(View.GONE);
                tvAddress.setText(currentLocation);
                tvAddress.setVisibility(View.VISIBLE);

                if(!btnProceed.isEnabled())
                    btnProceed.setEnabled(true);
            }

        }
        else
            showToast("Something went wrong");
    }

    public void showToast(String message)
    {
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        locationHelper.onActivityResult(requestCode,resultCode,data);
    }


    @Override
    protected void onResume() {
        super.onResume();
        locationHelper.checkPlayServices();
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
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


    // Permission check functions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // redirects to utils
        locationHelper.onRequestPermissionsResult(requestCode,permissions,grantResults);

    }

}
