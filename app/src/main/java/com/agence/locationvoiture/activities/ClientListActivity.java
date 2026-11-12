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
import com.agence.locationvoiture.adapters.ClientAdapter;
import com.agence.locationvoiture.dao.ClientDAO;
import com.agence.locationvoiture.models.Client;
import com.agence.locationvoiture.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Liste des clients.
 *
 * CONTRÔLE D'ACCÈS PAR RÔLE :
 * - Admin    : accès complet (ajouter, modifier, supprimer, voir réservations)
 * - Employé  : peut ajouter et modifier, mais PAS supprimer
 * - Client   : ACCÈS INTERDIT — redirigé vers son dashboard
 */
public class ClientListActivity extends AppCompatActivity implements ClientAdapter.OnClientClickListener {

    private RecyclerView recyclerView;
    private ClientAdapter adapter;
    private ClientDAO clientDAO;
    private SessionManager sessionManager;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Les clients n'ont pas accès à la liste des clients
        if (sessionManager.isClient()) {
            Toast.makeText(this, "Accès non autorisé", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ClientDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_list_generic);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gestion des Clients");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        clientDAO    = new ClientDAO(this);
        recyclerView = findViewById(R.id.recycler_view);
        etSearch     = findViewById(R.id.et_search);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Admin et employé peuvent ajouter un client
        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, ClientFormActivity.class)));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                filterClients(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClients();
    }

    private void loadClients() {
        List<Client> list = clientDAO.getAllClients();
        if (adapter == null) {
            adapter = new ClientAdapter(list, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(list);
        }
    }

    private void filterClients(String query) {
        List<Client> filtered = query.isEmpty()
                ? clientDAO.getAllClients()
                : clientDAO.searchClients(query);
        adapter.updateList(filtered);
    }

    @Override
    public void onEditClick(Client client) {
        // Admin et Employé peuvent modifier
        Intent intent = new Intent(this, ClientFormActivity.class);
        intent.putExtra("client_id", client.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Client client) {
        // Seul l'Admin peut supprimer
        if (sessionManager.isEmploye()) {
            Toast.makeText(this, "Seul l'administrateur peut supprimer un client", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Supprimer le client")
                .setMessage("Supprimer " + client.getNomComplet() + " ?\nSes réservations seront également supprimées.")
                .setPositiveButton("Supprimer", (d, w) -> {
                    int rows = clientDAO.deleteClient(client.getId());
                    if (rows > 0) {
                        Toast.makeText(this, "Client supprimé", Toast.LENGTH_SHORT).show();
                        loadClients();
                    } else {
                        Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public void onViewReservations(Client client) {
        Intent intent = new Intent(this, ReservationListActivity.class);
        intent.putExtra("client_id", client.getId());
        intent.putExtra("client_nom", client.getNomComplet());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
