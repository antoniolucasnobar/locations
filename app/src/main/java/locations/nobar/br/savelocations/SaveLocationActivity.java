package locations.nobar.br.savelocations;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import locations.nobar.br.savelocations.LocationUtil.LocationHelper;

public class SaveLocationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,ActivityCompat.OnRequestPermissionsResultCallback, IEnderecoCarregado {

    public static final String PREFS_NAME = "meus-mandados.txt";

    private static final String TAG = "SAVE_LOCATIONS - ";
    @BindView(R.id.btnLocation)Button btnProceed;
    @BindView(R.id.tvAddress)TextView tvAddress;
    @BindView(R.id.tvEmpty)LinearLayout tvEmpty;
    @BindView(R.id.partName)EditText partName;
    @BindView(R.id.userName)EditText userName;
    @BindView(R.id.descriptionText)EditText descriptionText;
    @BindView(R.id.mandadoComsucesso) Switch mandadoComsucesso;
    @BindView(R.id.processNumber)EditText processNumber;
    @BindView(R.id.loggedUserInfo)TextView loggedUserInfo;
    @BindView(R.id.loggedUserGroup)TextView loggedUserGroup;

    private Location mLastLocation;
    private Address locationAddress;


    double latitude;
    double longitude;

    LocationHelper locationHelper;
    ConnectivityManager connectivityManager;

    ProgressBar progressBar;
    FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    FirebaseUser currentUser;
    private UserInformation currentUserInformation;
    private String group;
    AsyncTask<Void, Void, Void> carregarPlayServices ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_location);
        ButterKnife.bind(this);
        if (progressBar == null) {
            LinearLayout spinnerLayout = new LinearLayout(this);
            spinnerLayout.setGravity(Gravity.CENTER);
            addContentView(spinnerLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            progressBar = new ProgressBar(this);
            spinnerLayout.addView(progressBar);
        }
        group = "public";
        mContext = this;
        configurarAcoesTela();

        carregarConfiguracaoLocalizacao();

        carregarPlayServices = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressBar.setVisibility(View.VISIBLE);
                showToast("Carregando aplicação...");
            }

            @Override
            protected Void doInBackground(Void... voids) {
                connectivityManager
                        = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                carregarDadosFirebase();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Aplicação carregada!", Toast.LENGTH_SHORT).show();
            }
        };
        carregarPlayServices.execute();
    }

    private void carregarConfiguracaoLocalizacao() {
        if (locationHelper == null)
            locationHelper = LocationHelper.getInstance(SaveLocationActivity.this);
        locationHelper.checkpermission();
//        // check availability of play services
        if (locationHelper.checkPlayServices()) {
//            // Building the GoogleApi client
            locationHelper.buildGoogleApi();
        }
    }

    private void configurarAcoesTela() {
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
                btnProceed.setEnabled(false);
            }
        });
    }

    private void carregarDadosFirebase() {
        String versionName = "";
        carregarVersao();

        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null ){
            if (currentUser.isEmailVerified()){
                carregarInformacoesUsuario(currentUser);
            } else {
                showToast("Você deve verificar seu email antes de usar o sistema com login e senha. Por favor, verifique sua caixa de emails. Saindo...");
                firebaseAuth.signOut();
                UserInstance.getInstance().logout(SaveLocationActivity.this);
                finish();
            }
        } else {
            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            loggedUserGroup.setText("Grupo: " + group);
                            loggedUserGroup.setVisibility(View.VISIBLE);
                        }
                    }
            );
        }
    }

    private void carregarVersao() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        String versionName;
                        try {
                            versionName = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
                            TextView versionText = (TextView) findViewById(R.id.versionText);
                            versionText.setText("v." + versionName);
                            versionText.setVisibility(View.VISIBLE);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    private void carregarInformacoesUsuario(final FirebaseUser currentUser) {
        String uid = currentUser.getUid();
        if (UserInstance.getInstance().getCurrentUserInformation() == null) {

            // Restore preferences
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            String fileUid = settings.getString("uid", "");
            if (fileUid.equalsIgnoreCase(currentUser.getUid())){
                String userName = settings.getString("user-name", "");
                String group = settings.getString("group", "public");
                UserInformation userInformation = new UserInformation(userName, group);
                UserInstance.getInstance().setCurrentUserInformation(userInformation);
                UserInstance.getInstance().setCurrentUser(currentUser);
                currentUserInformation = userInformation;
                dadosUsuarioNaTela(currentUserInformation);
            } else {
                Task<DocumentSnapshot> snapshotTask = db.collection("usersInformation").document(uid).get();
                snapshotTask.addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            currentUserInformation = task.getResult().toObject(UserInformation.class);
                            verificarCadastro();
                            UserInstance.getInstance().setCurrentUserInformation(currentUserInformation);
                            dadosUsuarioNaTela(currentUserInformation);
                            group = currentUserInformation.grupo;
                            if ("public".equalsIgnoreCase(group)) {
                                String grupoPeloEmail = ProfileActivity.recuperarGrupoPeloEmail(currentUser.getEmail());
                                if (grupoPeloEmail != null && !grupoPeloEmail.trim().isEmpty()) {
                                    group = grupoPeloEmail;
                                }
                            }
                            gravarDadosUsuarioLogado();
                        } else {
                            showToast("Por favor complete seu cadastro...");
                            goToLogin();
                            UserInstance.getInstance().logout(SaveLocationActivity.this);
                        }
                    }
                });
            }
        } else {
            currentUserInformation = UserInstance.getInstance().getCurrentUserInformation();
            verificarCadastro();
            dadosUsuarioNaTela(currentUserInformation);
        }
    }

    private void verificarCadastro() {
        if (currentUserInformation.nome == null || currentUserInformation.nome.trim().isEmpty()){
            showToast("Por favor complete seu cadastro...");
            goToLogin();
            UserInstance.getInstance().logout(SaveLocationActivity.this);
        }
    }

    private void dadosUsuarioNaTela(final UserInformation currentUserInformation) {
        runOnUiThread(new Runnable() {
            public void run() {
                group = currentUserInformation.grupo;
                loggedUserInfo.setText("Usuário: " + currentUserInformation.nome);
                loggedUserInfo.setVisibility(View.VISIBLE);
                loggedUserGroup.setText("Grupo: " + currentUserInformation.grupo);
                loggedUserGroup.setVisibility(View.VISIBLE);
                userName.setText(currentUserInformation.nome);
                userName.setVisibility(View.GONE);//o nome virá do cadastro do usuário
            }
        });
    }

    // Make sure this is the method with just `Bundle` as the signature
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }



    public void saveLocation(View view) {
        locationHelper.stopLocationUpdates();
        showToast("Salvando Localização...");
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
        location.put("timestamp", FieldValue.serverTimestamp());
        location.put("timestamp_client", DateFormat.getDateTimeInstance().format(new Date()));
        if (locationAddress != null) {
            String city = locationAddress.getSubAdminArea();
            String state = locationAddress.getAdminArea();
            location.put("postal-code", locationAddress.getPostalCode());
            location.put("location_debug", locationAddress.toString());
            location.put("city", city);
            location.put("state", state);
            location.put("address", locationAddress.getAddressLine(0));
        }
        if (currentUser != null ) {
            location.put("email", currentUser.getEmail());
            location.put("uid", currentUser.getUid());
            if (currentUserInformation != null) {
                location.put("user-name", currentUserInformation.nome);
                location.put("group", currentUserInformation.grupo);
            } else {
                location.put("group", group);
            }
        }



        CollectionReference places = db.collection("groups").document(group).collection("places");

// Add a new document with a generated ID
        places.add(location)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        showToast("Lugar salvo com sucesso: " + tvAddress.getText());
                        progressBar.setVisibility(View.GONE);     // To Hide ProgressBar
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error adding document", e);
                        showToast("Erro ao salvar: " + e.getLocalizedMessage());
                        progressBar.setVisibility(View.GONE);     // To Hide ProgressBar
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        finish();
                    }
                });
                startActivity(new Intent(this, SaveLocationActivity.class));
    }

    private boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void goToMap(View view) {
        progressBar.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("group", group);
        if (!isNetworkAvailable()){
            Toast.makeText(this, "O serviço de pesquisa só funciona com internet. Por favor, conecte-se a uma rede.", Toast.LENGTH_SHORT).show();
        } else {
            showToast("Carregando Mapa...");
            startActivity(intent);
        }
        progressBar.setVisibility(View.GONE);
    }
    public void goToLogin(View view) {
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, RegisterLoginActivity.class);
        startActivity(intent);
    }

    public Activity getActivity(){
        return this;
    }
    @Override
    public void onEnderecoCarregado(Location location){
        mLastLocation=location;

        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            getAddress();

        } else {

            if(btnProceed.isEnabled())
                btnProceed.setEnabled(false);

            showToast("Couldn't get the location. Make sure location is enabled on the device");
        }
        progressBar.setVisibility(View.GONE);
    }

    public void loadLocation() {
        progressBar.setVisibility(View.VISIBLE);
        showToast("Carregando localização...");

        locationHelper.buildGoogleApi();

        locationHelper.getLocation(this);
    }

    Context mContext;


    public void getAddress()
    {

        locationAddress=locationHelper.getAddress(latitude,longitude);

        String currentLocation = "Endereço não definido. Tente novamente.";
        if(locationAddress!=null)
        {

            String address = locationAddress.getAddressLine(0);
            String address1 = locationAddress.getAddressLine(1);
            String city = locationAddress.getSubAdminArea();
            String state = locationAddress.getAdminArea();
            String country = locationAddress.getCountryName();
            String postalCode = locationAddress.getPostalCode();



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

            }

        }
        else {
            showToast("Não foi possível recuperar o endereço a partir das coordenadas. Mas isso não impede salvar a localização.");
            currentLocation = "Latitude: " + latitude + "\nLongitude: " + longitude;
//            showToast("Tentando recuperar localização offline..");
//            getLocationOffline();
        }
        tvEmpty.setVisibility(View.GONE);
        tvAddress.setText(currentLocation);
        tvAddress.setVisibility(View.VISIBLE);

        if(!btnProceed.isEnabled())
            btnProceed.setEnabled(true);
    }

    public void showToast(final String message)
    {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              Toast.makeText(SaveLocationActivity.this,message, Toast.LENGTH_SHORT).show();
                          }
                      }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        locationHelper.onActivityResult(requestCode,resultCode,data);
    }


    @Override
    protected void onResume() {
        super.onResume();
//        isLocationEnabled();
        //carregarConfiguracaoLocalizacao();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gravarDadosUsuarioLogado();
    }

    private void gravarDadosUsuarioLogado() {
        if (currentUserInformation != null) {
            Log.i("MEUS-Mandados", "gravando dados");
            // We need an Editor object to make preference changes.
            // All objects are from android.context.Context
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("uid", currentUser.getUid());
            editor.putString("user-name", currentUserInformation.nome);
            editor.putString("group", currentUserInformation.grupo);

            // Commit the edits!
            editor.commit();
        }
    }
}
