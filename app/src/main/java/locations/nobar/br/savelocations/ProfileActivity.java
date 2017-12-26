package locations.nobar.br.savelocations;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProfileActivity extends AppCompatActivity {

    @BindView(R.id.email)TextView email;
    @BindView(R.id.groupName)TextView grupo;
    @BindView(R.id.nomeText)EditText nome;

    private FirebaseAuth firebaseAuth;
    FirebaseUser currentUser;
    FirebaseFirestore db;
    UserInformation userInformation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);
        firebaseAuth = FirebaseAuth.getInstance();
        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        userInformation = new UserInformation();
        recuperarDados(currentUser.getUid());
        email.setText("Email: " + currentUser.getEmail());
    }

    public void logout(View view) {
        firebaseAuth.signOut();
        finish();
        startActivity(new Intent(getApplicationContext(), SaveLocationActivity.class));
    }

    public void salvarInformacoes(View view) {
        String nome = this.nome.getText().toString().trim();
        userInformation.nome = nome;
        db.collection("usersInformation").document(currentUser.getUid())
            .set(userInformation, SetOptions.merge())
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                           @Override
                                           public void onComplete(@NonNull Task<Void> task) {
                   if (task.isSuccessful()){
                       Toast.makeText(getApplicationContext(), "Informações atualizadas com sucesso.", Toast.LENGTH_LONG).show();
                   } else{
                       Toast.makeText(getApplicationContext(), "Erro ao atualizar informações: " + task.getException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                   }
               }
             });
    }

    public static String recuperarGrupoPeloEmail(String email){
        int arroba = email.indexOf("@");
        String domain = email.substring(arroba+1);
        int groupIndex = domain.indexOf(".");
        String group = domain.substring(0,groupIndex);
        return group;
    }

    private void recuperarDados(String uid){
        Task<DocumentSnapshot> snapshotTask = db.collection("usersInformation").document(uid).get();
        snapshotTask.addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()){
                    userInformation = task.getResult().toObject(UserInformation.class);
                    if (userInformation.grupo == null) {
                        userInformation.grupo = recuperarGrupoPeloEmail(currentUser.getEmail());
                    }
                    grupo.setText("Grupo: " + userInformation.grupo);
                    nome.setText(userInformation.nome);
                }
            }
        });
    }

}
