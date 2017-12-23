package locations.nobar.br.savelocations;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RegisterLoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    @BindView(R.id.botaoCadastro)Button botaoCadastro;
    @BindView(R.id.botaoLogin)Button botaoLogin;
    @BindView(R.id.mensagemAlternarParaCadastro)TextView alternarParaCadastro;
    @BindView(R.id.mensagemAlternarParaLogin)TextView alternarParaLogin;
    @BindView(R.id.emailText)EditText email;
    @BindView(R.id.senhaText)EditText senha;
    ProgressBar progressBar;
    private boolean isLoginMode;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_login);
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null){
            Intent intent = new Intent(this, ProfileActivity.class);
            finish();
            startActivity(intent);
        }
        db = FirebaseFirestore.getInstance();

        isLoginMode = true;
        ButterKnife.bind(this);
        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100,100);
        params.gravity = Gravity.CENTER;
        LinearLayout layout = findViewById(R.id.profileLayout);
        layout.addView(progressBar,params);
        progressBar.setVisibility(View.GONE);
    }

    private void iniciarProgressBar(){
        progressBar.setVisibility(View.VISIBLE);  //To show ProgressBar
    }

    private void encerrarProgressBar(){
        progressBar.setVisibility(View.GONE);
    }

    private void realizarAcao(IAcao tipoAcao){
        iniciarProgressBar();
        String mPassword = senha.getText().toString().trim();
        String mEmail = email.getText().toString().trim();
        if (validarCampos(mEmail, mPassword)) {
            tipoAcao.executarAcao(mEmail, mPassword);
        } else {
            Toast.makeText(getApplicationContext(), "Erro ao validar os campos.", Toast.LENGTH_SHORT).show();
            encerrarProgressBar();
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    private boolean validarCampos(String mEmail, String mPassword) {
        return isEmailValid(mEmail) && isPasswordValid(mPassword);
    }

    public void executeAction(View view) {
        IAcao iAcao;
        if (isLoginMode) {
            iAcao = new LoginMode();
        } else {
            iAcao = new CadastroMode();
        }
        realizarAcao(iAcao);
    }

    public void alternateToLoginMode(View view) {
        botaoLogin.setVisibility(View.VISIBLE);
        botaoCadastro.setVisibility(View.GONE);
        alternarParaLogin.setVisibility(View.GONE);
        alternarParaCadastro.setVisibility(View.VISIBLE);
        isLoginMode = true;
    }

    public void alternateToRegisterMode(View view) {
        botaoLogin.setVisibility(View.GONE);
        botaoCadastro.setVisibility(View.VISIBLE);
        alternarParaLogin.setVisibility(View.VISIBLE);
        alternarParaCadastro.setVisibility(View.GONE);
        isLoginMode = false;
    }
    
    interface IAcao {
        String getMensagem();
        void executarAcao(String email, String password);

    }

    class LoginMode implements IAcao {

        String mensagem;

        @Override
        public String getMensagem() {
            return mensagem;
        }

        @Override
        public void executarAcao(String email, String password) {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                mensagem = " Login efetuado com sucesso.";
                                Log.i(" INFO: ", mensagem);
                                Intent intent = new Intent(getApplicationContext(), SaveLocationActivity.class);
                                finish();
                                startActivity(intent);
                            } else {
                                mensagem = " Erro no Login: " + task.getException().getLocalizedMessage();
                                Log.e(" ERRO: ", mensagem);
                            }
                            Toast.makeText(getApplicationContext(), mensagem, Toast.LENGTH_SHORT).show();
                            encerrarProgressBar();

                        }
                    });

        }
    }

    class CadastroMode implements IAcao{

        String mensagem;

        @Override
        public String getMensagem() {
            return mensagem;
        }

        @Override
        public void executarAcao(String email, String password) {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                Log.i(" LOCATIONS ", " Cadastro com sucesso.");
                                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                                currentUser.sendEmailVerification();
                                UserInformation userInformation = new UserInformation(null, ProfileActivity.recuperarGrupoPeloEmail(currentUser.getEmail()));
                                db.collection("usersInformation").document(currentUser.getUid()).set(userInformation);
                                mensagem = " Cadastro com sucesso. Por favor verifique sua caixa de email";
                                Log.i(" INFO: ", mensagem);
                                finish();
                                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                            } else {
                                mensagem = " Erro ao cadastrar: " + task.getException().getLocalizedMessage();
                                Log.e(" ERRO: ", mensagem);
                            }
                            Toast.makeText(getApplicationContext(), mensagem, Toast.LENGTH_SHORT).show();
                            encerrarProgressBar();
                        }
                    });

        }
    }
}
