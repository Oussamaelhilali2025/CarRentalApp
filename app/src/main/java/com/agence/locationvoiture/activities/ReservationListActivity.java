package com.agence.locationvoiture.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.adapters.ReservationAdapter;
import com.agence.locationvoiture.dao.ClientDAO;
import com.agence.locationvoiture.dao.ReservationDAO;
import com.agence.locationvoiture.models.Client;
import com.agence.locationvoiture.models.Reservation;
import com.agence.locationvoiture.services.NotificationService;
import com.agence.locationvoiture.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Liste des réservations avec filtrage par statut.
 *
 * CONTRÔLE D'ACCÈS PAR RÔLE :
 * - Admin    : toutes les réservations, peut annuler/terminer/supprimer
 * - Employé  : toutes les réservations, peut annuler/terminer (PAS supprimer)
 * - Client   : UNIQUEMENT ses propres réservations
 *              (retrouvées via utilisateur_id → client_id dans la table clients)
 */
public class ReservationListActivity extends AppCompatActivity
        implements ReservationAdapter.OnReservationClickListener {

    private RecyclerView recyclerView;
    private ReservationAdapter adapter;
    private ReservationDAO reservationDAO;
    private SessionManager sessionManager;
    private NotificationService notifService;
    private Spinner spinnerStatut;
    private TextView tvEmpty;

    // -1 = pas de filtre client (admin/employé voit tout)
    // >0 = filtrer par ce client_id
    private int filterClientId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_list);

        reservationDAO = new ReservationDAO(this);
        sessionManager = new SessionManager(this);
        notifService   = new NotificationService(this);

        // Résoudre le client_id selon le rôle
        if (sessionManager.isClient()) {
            // Le client voit uniquement ses réservations :
            // on retrouve sa fiche client via utilisateur_id
            ClientDAO clientDAO = new ClientDAO(this);
            Client monProfil = clientDAO.getClientByUserId(sessionManager.getUserId());
            if (monProfil != null) {
                filterClientId = monProfil.getId();
            } else {
                // Client sans fiche liée : afficher message
                filterClientId = -99; // valeur sentinelle "aucune fiche"
            }
        } else {
            // Admin/Employé : éventuellement filtrer par un client spécifique
            filterClientId = getIntent().getIntExtra("client_id", -1);
        }

        String clientNom = getIntent().getStringExtra("client_nom");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String title;
            if (sessionManager.isClient()) {
                title = "Mes Réservations";
            } else if (filterClientId > 0 && clientNom != null) {
                title = "Réservations – " + clientNom;
            } else {
                title = "Gestion des Réservations";
            }
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView  = findViewById(R.id.recycler_view);
        spinnerStatut = findViewById(R.id.spinner_statut);
        tvEmpty       = findViewById(R.id.tv_empty);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Tous les rôles peuvent créer une réservation
        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, ReservationFormActivity.class)));

        setupSpinner();
    }

    private void setupSpinner() {
        String[] filtres = {"Toutes", "En cours", "Terminées", "Annulées"};
        spinnerStatut.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filtres));
        spinnerStatut.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                loadReservations(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReservations(spinnerStatut != null ? spinnerStatut.getSelectedItemPosition() : 0);
    }

    private void loadReservations(int filtreIndex) {
        List<Reservation> list;

        if (filterClientId == -99) {
            // Client sans fiche liée
            list = new ArrayList<>();
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Votre profil client n'est pas encore configuré.\nContactez l'agence.");
            }
        } else if (filterClientId > 0) {
            // Client connecté ou admin filtrant par client
            List<Reservation> all = reservationDAO.getReservationsByClient(filterClientId);
            list = filtrerParStatut(all, filtreIndex);
        } else {
            // Admin/Employé : toutes les réservations
            switch (filtreIndex) {
                case 1: list = reservationDAO.getReservationsByStatut(Reservation.STATUT_EN_COURS);  break;
                case 2: list = reservationDAO.getReservationsByStatut(Reservation.STATUT_TERMINEE); break;
                case 3: list = reservationDAO.getReservationsByStatut(Reservation.STATUT_ANNULEE);  break;
                default: list = reservationDAO.getAllReservations(); break;
            }
        }

        if (tvEmpty != null) {
            tvEmpty.setVisibility((list.isEmpty() && filterClientId != -99) ? View.VISIBLE : View.GONE);
            if (list.isEmpty() && filterClientId != -99) {
                tvEmpty.setText(sessionManager.isClient()
                        ? "Vous n'avez aucune réservation pour l'instant."
                        : "Aucune réservation trouvée.");
            }
        }

        if (adapter == null) {
            adapter = new ReservationAdapter(list, this, sessionManager.isClient());
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(list);
        }
    }

    private List<Reservation> filtrerParStatut(List<Reservation> all, int index) {
        if (index == 0) return all;
        String statut;
        switch (index) {
            case 1: statut = Reservation.STATUT_EN_COURS;  break;
            case 2: statut = Reservation.STATUT_TERMINEE;  break;
            case 3: statut = Reservation.STATUT_ANNULEE;   break;
            default: return all;
        }
        List<Reservation> filtered = new ArrayList<>();
        for (Reservation r : all) { if (statut.equals(r.getStatut())) filtered.add(r); }
        return filtered;
    }

    @Override
    public void onDetailClick(Reservation reservation) {
        Intent intent = new Intent(this, ReservationDetailActivity.class);
        intent.putExtra("reservation_id", reservation.getId());
        startActivity(intent);
    }

    @Override
    public void onAnnulerClick(Reservation reservation) {
        // Tous les rôles peuvent annuler une réservation en cours
        if (Reservation.STATUT_ANNULEE.equals(reservation.getStatut())) {
            Toast.makeText(this, "Cette réservation est déjà annulée", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Annuler la réservation")
                .setMessage("Annuler la réservation #" + reservation.getId() +
                        " ?\nLa voiture sera remise disponible.")
                .setPositiveButton("Confirmer", (d, w) -> {
                    boolean ok = reservationDAO.annulerReservation(reservation.getId());
                    if (ok) {
                        notifService.notifierReservationAnnulee(
                                reservation.getId(), reservation.getVoitureNom());
                        Toast.makeText(this, "Réservation annulée", Toast.LENGTH_SHORT).show();
                        loadReservations(spinnerStatut.getSelectedItemPosition());
                    } else {
                        Toast.makeText(this, "Erreur lors de l'annulation", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Retour", null)
                .show();
    }

    @Override
    public void onTerminerClick(Reservation reservation) {
        // Admin et Employé uniquement
        if (sessionManager.isClient()) {
            Toast.makeText(this, "Action non autorisée", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Terminer la réservation")
                .setMessage("Marquer la réservation #" + reservation.getId() + " comme terminée ?")
                .setPositiveButton("Terminer", (d, w) -> {
                    boolean ok = reservationDAO.terminerReservation(reservation.getId());
                    if (ok) {
                        Toast.makeText(this, "Réservation terminée", Toast.LENGTH_SHORT).show();
                        loadReservations(spinnerStatut.getSelectedItemPosition());
                    } else {
                        Toast.makeText(this, "Erreur", Toast.LENGTH_SHORT).show();
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
