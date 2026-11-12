package com.agence.locationvoiture.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.adapters.VoitureAdapter;
import com.agence.locationvoiture.dao.VoitureDAO;
import com.agence.locationvoiture.models.Voiture;
import com.agence.locationvoiture.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Liste des voitures.
 *
 * CONTRÔLE D'ACCÈS PAR RÔLE :
 * - Admin    : voir, ajouter, modifier, supprimer
 * - Employé  : voir, ajouter, modifier (PAS supprimer)
 * - Client   : voir uniquement (pas de FAB, pas de boutons edit/delete)
 */
public class VoitureListActivity extends AppCompatActivity
        implements VoitureAdapter.OnVoitureClickListener {

    private RecyclerView recyclerView;
    private VoitureAdapter adapter;
    private VoitureDAO voitureDAO;
    private SessionManager sessionManager;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_generic);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                    sessionManager != null && new SessionManager(this).isClient()
                    ? "Voitures Disponibles" : "Gestion des Voitures");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        voitureDAO     = new VoitureDAO(this);
        sessionManager = new SessionManager(this);

        // Re-set title after sessionManager is initialized
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                    sessionManager.isClient() ? "Voitures Disponibles" : "Gestion des Voitures");
        }

        recyclerView = findViewById(R.id.recycler_view);
        etSearch     = findViewById(R.id.et_search);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Clients : lecture seule, pas de FAB
        if (sessionManager.isClient()) {
            fabAdd.hide();
        } else {
            // Admin et Employé peuvent ajouter
            fabAdd.setOnClickListener(v ->
                    startActivity(new Intent(this, VoitureFormActivity.class)));
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                filterVoitures(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVoitures();
    }

    private void loadVoitures() {
        // Clients voient uniquement les voitures disponibles
        List<Voiture> list = sessionManager.isClient()
                ? voitureDAO.getVoituresDisponibles()
                : voitureDAO.getAllVoitures();

        if (adapter == null) {
            // isReadOnly = true pour les clients (pas de boutons edit/delete)
            adapter = new VoitureAdapter(list, this, sessionManager.isClient());
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(list);
        }
    }

    private void filterVoitures(String query) {
        List<Voiture> filtered = query.isEmpty()
                ? (sessionManager.isClient() ? voitureDAO.getVoituresDisponibles() : voitureDAO.getAllVoitures())
                : voitureDAO.searchVoitures(query);
        adapter.updateList(filtered);
    }

    @Override
    public void onEditClick(Voiture voiture) {
        // Admin et Employé uniquement
        if (sessionManager.isClient()) return;
        Intent intent = new Intent(this, VoitureFormActivity.class);
        intent.putExtra("voiture_id", voiture.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Voiture voiture) {
        // Admin uniquement
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Seul l'administrateur peut supprimer une voiture", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Supprimer la voiture")
                .setMessage("Supprimer " + voiture.getNomComplet() + " ?\nCette action est irréversible.")
                .setPositiveButton("Supprimer", (d, w) -> {
                    int rows = voitureDAO.deleteVoiture(voiture.getId());
                    if (rows > 0) {
                        Toast.makeText(this, "Voiture supprimée", Toast.LENGTH_SHORT).show();
                        loadVoitures();
                    } else {
                        Toast.makeText(this, "Impossible : des réservations existent pour cette voiture", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
