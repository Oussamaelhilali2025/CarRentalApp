package com.agence.locationvoiture.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.dao.ClientDAO;
import com.agence.locationvoiture.dao.PaiementDAO;
import com.agence.locationvoiture.dao.ReservationDAO;
import com.agence.locationvoiture.dao.VoitureDAO;
import com.agence.locationvoiture.models.Client;
import com.agence.locationvoiture.utils.SessionManager;

/**
 * Dashboard dédié au client connecté.
 * Interface simplifiée — séparée du dashboard admin/employé.
 *
 * Affiche uniquement les données du client connecté :
 * - Ses réservations en cours (filtrées via utilisateur_id → client_id)
 * - Les voitures disponibles
 * - Accès : réserver, payer en ligne, localiser les voitures
 */
public class ClientDashboardActivity extends AppCompatActivity {

    private SessionManager    sessionManager;
    private VoitureDAO        voitureDAO;
    private ReservationDAO    reservationDAO;
    private ClientDAO         clientDAO;
    private PaiementDAO       paiementDAO;

    private TextView tvWelcome, tvVoituresDispo, tvMesReservations, tvMesPaiements;
    private TextView tvNomComplet, tvEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        // Rediriger admin/employé vers le dashboard principal
        if (!sessionManager.isClient()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_client_dashboard);

        voitureDAO     = new VoitureDAO(this);
        reservationDAO = new ReservationDAO(this);
        clientDAO      = new ClientDAO(this);
        paiementDAO    = new PaiementDAO(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mon Espace Client");
        }

        initViews();
        setupCards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void initViews() {
        tvWelcome         = findViewById(R.id.tv_welcome);
        tvVoituresDispo   = findViewById(R.id.tv_voitures_dispo);
        tvMesReservations = findViewById(R.id.tv_mes_reservations);
        tvNomComplet      = findViewById(R.id.tv_nom_complet);
        tvEmail           = findViewById(R.id.tv_email);

        tvWelcome.setText("Bienvenue, " + sessionManager.getUsername() + " 👋");
    }

    private void loadStats() {
        // Voitures disponibles
        int nbDispo = voitureDAO.countDisponibles();
        tvVoituresDispo.setText(String.valueOf(nbDispo));

        // Réservations en cours du client connecté
        Client monProfil = clientDAO.getClientByUserId(sessionManager.getUserId());
        if (monProfil != null) {
            // Afficher le nom et email du client
            if (tvNomComplet != null) tvNomComplet.setText(monProfil.getNomComplet());
            if (tvEmail != null) tvEmail.setText(monProfil.getEmail());

            // Compter ses réservations en cours
            int nbMesRes = reservationDAO.countEnCoursByClient(monProfil.getId());
            tvMesReservations.setText(String.valueOf(nbMesRes));
        } else {
            tvMesReservations.setText("0");
            if (tvNomComplet != null) tvNomComplet.setText(sessionManager.getUsername());
            if (tvEmail != null) tvEmail.setText("Profil non configuré");
        }
    }

    private void setupCards() {
        CardView cardVoitures      = findViewById(R.id.card_voitures_dispo);
        CardView cardReservations  = findViewById(R.id.card_mes_reservations);
        CardView cardNouvelle      = findViewById(R.id.card_nouvelle_reservation);
        CardView cardPaiement      = findViewById(R.id.card_paiement_en_ligne);
        CardView cardGeo           = findViewById(R.id.card_geolocalisation);
        CardView cardHistorique    = findViewById(R.id.card_historique_paiements);

        // Voir les voitures disponibles
        cardVoitures.setOnClickListener(v ->
                startActivity(new Intent(this, VoitureListActivity.class)));

        // Mes réservations (filtrées automatiquement au client connecté)
        cardReservations.setOnClickListener(v ->
                startActivity(new Intent(this, ReservationListActivity.class)));

        // Nouvelle réservation
        cardNouvelle.setOnClickListener(v ->
                startActivity(new Intent(this, ReservationFormActivity.class)));

        // Paiement en ligne — ouvre directement le module de paiement
        // (mode sélection : le client choisit parmi ses réservations impayées)
        cardPaiement.setOnClickListener(v ->
                startActivity(new Intent(this, PaiementEnLigneActivity.class)));

        // Géolocalisation
        cardGeo.setOnClickListener(v ->
                startActivity(new Intent(this, GeolocationActivity.class)));

        // Historique des paiements (filtrés automatiquement au client)
        cardHistorique.setOnClickListener(v ->
                startActivity(new Intent(this, PaiementListActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Déconnexion")
                    .setMessage("Voulez-vous vous déconnecter ?")
                    .setPositiveButton("Oui", (d, w) -> {
                        sessionManager.logout();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
