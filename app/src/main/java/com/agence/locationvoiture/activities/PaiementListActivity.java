package com.agence.locationvoiture.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.adapters.PaiementAdapter;
import com.agence.locationvoiture.dao.PaiementDAO;
import com.agence.locationvoiture.models.Paiement;
import com.agence.locationvoiture.utils.SessionManager;

import java.util.List;

/**
 * Historique des paiements.
 *
 * CONTRÔLE D'ACCÈS PAR RÔLE :
 * - Admin    : TOUS les paiements (global) + peut supprimer
 * - Employé  : TOUS les paiements (global), PAS de suppression
 * - Client   : UNIQUEMENT ses propres paiements
 *              (filtrés via utilisateur_id → client_id → reservation_id → paiement)
 */
public class PaiementListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PaiementDAO paiementDAO;
    private SessionManager sessionManager;
    private TextView tvTotal, tvTitre, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paiement_list);

        paiementDAO    = new PaiementDAO(this);
        sessionManager = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                    sessionManager.isClient() ? "Mes Paiements" : "Historique des Paiements");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recycler_view);
        tvTotal      = findViewById(R.id.tv_total_global);
        tvTitre      = findViewById(R.id.tv_titre_total);
        tvEmpty      = findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPaiements();
    }

    private void loadPaiements() {
        List<Paiement> list;

        if (sessionManager.isClient()) {
            // Client : uniquement ses propres paiements filtrés par utilisateur_id
            list = paiementDAO.getPaiementsByUserId(sessionManager.getUserId());
            tvTitre.setText("Total payé :");
        } else {
            // Admin / Employé : tous les paiements
            list = paiementDAO.getAllPaiements();
            tvTitre.setText("Total encaissé :");
        }

        // Total
        double total = 0;
        for (Paiement p : list) total += p.getMontant();
        tvTotal.setText(String.format("%.2f MAD", total));

        // Message vide
        if (tvEmpty != null) {
            if (list.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(sessionManager.isClient()
                        ? "Vous n'avez aucun paiement enregistré."
                        : "Aucun paiement enregistré.");
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        }

        // Admin peut supprimer, employé et client ne peuvent pas
        PaiementAdapter.OnPaiementDeleteListener deleteListener = sessionManager.isAdmin()
                ? p -> {
                    paiementDAO.deletePaiement(p.getId());
                    Toast.makeText(this, "Paiement supprimé", Toast.LENGTH_SHORT).show();
                    loadPaiements();
                }
                : null; // null = bouton suppression masqué dans l'adapter

        PaiementAdapter adapter = new PaiementAdapter(list, deleteListener);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
