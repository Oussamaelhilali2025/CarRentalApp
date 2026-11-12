package com.agence.locationvoiture.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.dao.UtilisateurDAO;
import com.agence.locationvoiture.models.Utilisateur;
import com.agence.locationvoiture.utils.SessionManager;

/**
 * Écran de connexion.
 * Route les clients vers ClientDashboardActivity, admin/employé vers MainActivity.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvGoRegister;

    private UtilisateurDAO utilisateurDAO;
    private SessionManager sessionManager;

    // Launcher pour demander la permission notifications
    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        utilisateurDAO = new UtilisateurDAO(this);
        sessionManager = new SessionManager(this);

        etUsername   = findViewById(R.id.et_username);
        etPassword   = findViewById(R.id.et_password);
        btnLogin     = findViewById(R.id.btn_login);
        tvGoRegister = findViewById(R.id.tv_go_register);

        // Demander la permission de notification (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }

        btnLogin.setOnClickListener(v -> tentativeConnexion());
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void tentativeConnexion() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Nom d'utilisateur requis");
            etUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Mot de passe requis");
            etPassword.requestFocus();
            return;
        }

        Utilisateur user = utilisateurDAO.authentifier(username, password);
        if (user != null) {
            sessionManager.createSession(user.getId(), user.getUsername(), user.getRole());

            // Router selon le rôle
            Intent intent;
            if (user.isClient()) {
                intent = new Intent(this, ClientDashboardActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
            }
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
        }
    }
}
