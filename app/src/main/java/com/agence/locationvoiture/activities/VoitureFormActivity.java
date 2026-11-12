package com.agence.locationvoiture.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.dao.VoitureDAO;
import com.agence.locationvoiture.models.Voiture;
import com.agence.locationvoiture.utils.SessionManager;

import java.util.Calendar;

/**
 * Formulaire de création / modification d'une voiture.
 *
 * CONTRÔLE D'ACCÈS PAR RÔLE :
 * - Admin    : peut ajouter, modifier, changer disponibilité
 * - Employé  : peut ajouter et modifier (pas supprimer — géré dans VoitureListActivity)
 * - Client   : accès interdit (redirigé depuis VoitureListActivity, fab caché)
 */
public class VoitureFormActivity extends AppCompatActivity {

    private EditText etMarque, etModele, etAnnee, etPrixJour;
    private CheckBox cbDisponible;
    private Button btnSave;

    private VoitureDAO voitureDAO;
    private SessionManager sessionManager;
    private Voiture voitureToEdit = null;
    private boolean isEditMode    = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voiture_form);

        voitureDAO     = new VoitureDAO(this);
        sessionManager = new SessionManager(this);

        // Seuls admin et employé accèdent à ce formulaire
        if (sessionManager.isClient()) {
            Toast.makeText(this, "Accès non autorisé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etMarque     = findViewById(R.id.et_marque);
        etModele     = findViewById(R.id.et_modele);
        etAnnee      = findViewById(R.id.et_annee);
        etPrixJour   = findViewById(R.id.et_prix_jour);
        cbDisponible = findViewById(R.id.cb_disponible);
        btnSave      = findViewById(R.id.btn_save);

        int voitureId = getIntent().getIntExtra("voiture_id", -1);
        if (voitureId != -1) {
            isEditMode    = true;
            voitureToEdit = voitureDAO.getVoitureById(voitureId);
            prefillForm();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEditMode ? "Modifier la Voiture" : "Ajouter une Voiture");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnSave.setOnClickListener(v -> sauvegarder());
    }

    private void prefillForm() {
        if (voitureToEdit == null) return;
        etMarque.setText(voitureToEdit.getMarque());
        etModele.setText(voitureToEdit.getModele());
        etAnnee.setText(String.valueOf(voitureToEdit.getAnnee()));
        etPrixJour.setText(String.valueOf(voitureToEdit.getPrixJour()));
        cbDisponible.setChecked(voitureToEdit.isDisponible());
    }

    private void sauvegarder() {
        String marque   = etMarque.getText().toString().trim();
        String modele   = etModele.getText().toString().trim();
        String anneeStr = etAnnee.getText().toString().trim();
        String prixStr  = etPrixJour.getText().toString().trim();

        if (TextUtils.isEmpty(marque))   { etMarque.setError("Marque requise"); return; }
        if (TextUtils.isEmpty(modele))   { etModele.setError("Modèle requis"); return; }
        if (TextUtils.isEmpty(anneeStr)) { etAnnee.setError("Année requise"); return; }
        if (TextUtils.isEmpty(prixStr))  { etPrixJour.setError("Prix requis"); return; }

        int annee;
        double prix;
        try {
            annee = Integer.parseInt(anneeStr);
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if (annee < 1990 || annee > currentYear + 1) {
                etAnnee.setError("Année invalide (1990–" + (currentYear + 1) + ")");
                return;
            }
        } catch (NumberFormatException e) {
            etAnnee.setError("Année invalide"); return;
        }
        try {
            prix = Double.parseDouble(prixStr);
            if (prix <= 0) { etPrixJour.setError("Le prix doit être positif"); return; }
        } catch (NumberFormatException e) {
            etPrixJour.setError("Prix invalide"); return;
        }

        Voiture voiture = isEditMode ? voitureToEdit : new Voiture();
        voiture.setMarque(marque);
        voiture.setModele(modele);
        voiture.setAnnee(annee);
        voiture.setPrixJour(prix);
        voiture.setDisponible(cbDisponible.isChecked());

        if (isEditMode) {
            int rows = voitureDAO.updateVoiture(voiture);
            if (rows > 0) {
                Toast.makeText(this, "Voiture mise à jour", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
            }
        } else {
            long id = voitureDAO.addVoiture(voiture);
            if (id > 0) {
                Toast.makeText(this, "Voiture ajoutée avec succès", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
