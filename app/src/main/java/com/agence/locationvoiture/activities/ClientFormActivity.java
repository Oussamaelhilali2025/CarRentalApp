package com.agence.locationvoiture.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.dao.ClientDAO;
import com.agence.locationvoiture.models.Client;

public class ClientFormActivity extends AppCompatActivity {

    private EditText etNom, etPrenom, etEmail, etTelephone;
    private Button btnSave;
    private ClientDAO clientDAO;
    private Client clientToEdit = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_form);

        clientDAO    = new ClientDAO(this);
        etNom        = findViewById(R.id.et_nom);
        etPrenom     = findViewById(R.id.et_prenom);
        etEmail      = findViewById(R.id.et_email);
        etTelephone  = findViewById(R.id.et_telephone);
        btnSave      = findViewById(R.id.btn_save);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        int clientId = getIntent().getIntExtra("client_id", -1);
        if (clientId != -1) {
            isEditMode    = true;
            clientToEdit  = clientDAO.getClientById(clientId);
            prefillForm();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEditMode ? "Modifier le Client" : "Ajouter un Client");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnSave.setOnClickListener(v -> sauvegarder());
    }

    private void prefillForm() {
        if (clientToEdit == null) return;
        etNom.setText(clientToEdit.getNom());
        etPrenom.setText(clientToEdit.getPrenom());
        etEmail.setText(clientToEdit.getEmail());
        etTelephone.setText(clientToEdit.getTelephone());
    }

    private void sauvegarder() {
        String nom       = etNom.getText().toString().trim();
        String prenom    = etPrenom.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String telephone = etTelephone.getText().toString().trim();

        if (TextUtils.isEmpty(nom))       { etNom.setError("Nom requis"); return; }
        if (TextUtils.isEmpty(prenom))    { etPrenom.setError("Prénom requis"); return; }
        if (TextUtils.isEmpty(email))     { etEmail.setError("Email requis"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format email invalide"); return;
        }
        if (TextUtils.isEmpty(telephone)) { etTelephone.setError("Téléphone requis"); return; }
        if (telephone.length() < 10)      { etTelephone.setError("Numéro invalide (min 10 chiffres)"); return; }

        int excludeId = isEditMode ? clientToEdit.getId() : -1;
        if (clientDAO.emailExiste(email, excludeId)) {
            etEmail.setError("Cet email est déjà utilisé");
            return;
        }

        Client client = isEditMode ? clientToEdit : new Client();
        client.setNom(nom);
        client.setPrenom(prenom);
        client.setEmail(email);
        client.setTelephone(telephone);

        if (isEditMode) {
            int rows = clientDAO.updateClient(client);
            if (rows > 0) { Toast.makeText(this, "Client mis à jour", Toast.LENGTH_SHORT).show(); finish(); }
            else           Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
        } else {
            long id = clientDAO.addClient(client);
            if (id > 0) { Toast.makeText(this, "Client ajouté", Toast.LENGTH_SHORT).show(); finish(); }
            else          Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
