package com.agence.locationvoiture.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.dao.ClientDAO;
import com.agence.locationvoiture.dao.UtilisateurDAO;
import com.agence.locationvoiture.models.Client;
import com.agence.locationvoiture.models.Utilisateur;

/**
 * Écran d'inscription.
 * Crée simultanément :
 *  1. Un compte Utilisateur (username + password + role = "client")
 *  2. Une fiche Client (nom, prénom, email, téléphone) liée par utilisateur_id
 *
 * Ainsi, quand le client se connecte, ses réservations et paiements
 * sont retrouvés via la liaison client.utilisateur_id = utilisateur.id.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etConfirmPassword;
    private EditText etNom, etPrenom, etEmail, etTelephone;
    private Button btnRegister;

    private UtilisateurDAO utilisateurDAO;
    private ClientDAO clientDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        utilisateurDAO    = new UtilisateurDAO(this);
        clientDAO         = new ClientDAO(this);

        etUsername        = findViewById(R.id.et_username);
        etPassword        = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etNom             = findViewById(R.id.et_nom);
        etPrenom          = findViewById(R.id.et_prenom);
        etEmail           = findViewById(R.id.et_email);
        etTelephone       = findViewById(R.id.et_telephone);
        btnRegister       = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> inscrire());
        findViewById(R.id.tv_go_login).setOnClickListener(v -> finish());
    }

    private void inscrire() {
        String username   = etUsername.getText().toString().trim();
        String password   = etPassword.getText().toString().trim();
        String confirmPwd = etConfirmPassword.getText().toString().trim();
        String nom        = etNom.getText().toString().trim();
        String prenom     = etPrenom.getText().toString().trim();
        String email      = etEmail.getText().toString().trim();
        String telephone  = etTelephone.getText().toString().trim();

        // --- Validations ---
        if (TextUtils.isEmpty(nom))      { etNom.setError("Nom requis"); return; }
        if (TextUtils.isEmpty(prenom))   { etPrenom.setError("Prénom requis"); return; }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email valide requis"); return;
        }
        if (TextUtils.isEmpty(telephone) || telephone.length() < 8) {
            etTelephone.setError("Téléphone valide requis"); return;
        }
        if (TextUtils.isEmpty(username) || username.length() < 3) {
            etUsername.setError("Nom d'utilisateur (min. 3 caractères)"); return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Mot de passe (min. 6 caractères)"); return;
        }
        if (!password.equals(confirmPwd)) {
            etConfirmPassword.setError("Les mots de passe ne correspondent pas"); return;
        }
        if (utilisateurDAO.usernameExiste(username)) {
            etUsername.setError("Ce nom d'utilisateur est déjà pris"); return;
        }
        if (clientDAO.emailExiste(email)) {
            etEmail.setError("Un compte existe déjà avec cet email"); return;
        }

        // --- Créer le compte utilisateur ---
        Utilisateur newUser = new Utilisateur(username, password, "client");
        long userId = utilisateurDAO.addUtilisateur(newUser);

        if (userId <= 0) {
            Toast.makeText(this, "Erreur lors de la création du compte", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Créer la fiche client liée ---
        Client client = new Client(nom, prenom, email, telephone);
        client.setUtilisateurId((int) userId);
        long clientId = clientDAO.addClient(client);

        if (clientId > 0) {
            Toast.makeText(this, "Compte créé avec succès ! Bienvenue " + prenom + " !", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            // Rollback : supprimer l'utilisateur créé
            utilisateurDAO.deleteUtilisateur((int) userId);
            Toast.makeText(this, "Erreur lors de la création du profil", Toast.LENGTH_SHORT).show();
        }
    }
}
