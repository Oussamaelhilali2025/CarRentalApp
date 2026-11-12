package com.agence.locationvoiture.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.dao.ClientDAO;
import com.agence.locationvoiture.dao.ReservationDAO;
import com.agence.locationvoiture.dao.VoitureDAO;
import com.agence.locationvoiture.utils.SessionManager;

/**
 * Dashboard principal Admin / Employé.
 *
 * DIFFÉRENCES VISUELLES PAR RÔLE :
 * ┌─────────────────────────────────────────────────────┐
 * │ ADMIN                    │ EMPLOYÉ                  │
 * ├─────────────────────────────────────────────────────┤
 * │ Badge rouge "ADMIN"      │ Badge orange "EMPLOYÉ"   │
 * │ Section "Zone Admin"     │ Section masquée          │
 * │  · Gestion utilisateurs  │                          │
 * │  · Droits de suppression │                          │
 * │ Sous-titre Voitures :    │ Sous-titre Voitures :    │
 * │  "Ajouter, modifier,     │  "Ajouter, modifier"     │
 * │   supprimer"             │  (pas supprimer)         │
 * │ Sous-titre Paiements :   │ Sous-titre Paiements :   │
 * │  "Encaisser + supprimer" │  "Consulter uniquement"  │
 * └─────────────────────────────────────────────────────┘
 */
public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private VoitureDAO     voitureDAO;
    private ClientDAO      clientDAO;
    private ReservationDAO reservationDAO;

    private TextView tvWelcome, tvRoleBadge, tvNbVoitures, tvNbDispo, tvNbClients, tvNbReservations;
    private TextView tvSubVoitures, tvSubClients, tvSubPaiements;
    private LinearLayout sectionAdminOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (sessionManager.isClient()) {
            startActivity(new Intent(this, ClientDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        voitureDAO     = new VoitureDAO(this);
        clientDAO      = new ClientDAO(this);
        reservationDAO = new ReservationDAO(this);

        initViews();
        applyRoleUI();
        setupCards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void initViews() {
        tvWelcome         = findViewById(R.id.tv_welcome);
        tvRoleBadge       = findViewById(R.id.tv_role_badge);
        tvNbVoitures      = findViewById(R.id.tv_nb_voitures);
        tvNbDispo         = findViewById(R.id.tv_nb_dispo);
        tvNbClients       = findViewById(R.id.tv_nb_clients);
        tvNbReservations  = findViewById(R.id.tv_nb_reservations);
        tvSubVoitures     = findViewById(R.id.tv_sub_voitures);
        tvSubClients      = findViewById(R.id.tv_sub_clients);
        tvSubPaiements    = findViewById(R.id.tv_sub_paiements);
        sectionAdminOnly  = findViewById(R.id.section_admin_only);
    }

    /**
     * Adapte l'interface selon le rôle connecté.
     * Admin : badge rouge + section admin visible + sous-titres complets.
     * Employé : badge orange + section admin masquée + sous-titres restreints.
     */
    private void applyRoleUI() {
        boolean isAdmin = sessionManager.isAdmin();
        String username = sessionManager.getUsername();

        if (isAdmin) {
            tvWelcome.setText("Bonjour, " + username + " 👑");
            tvRoleBadge.setText("  ADMINISTRATEUR  ");
            tvRoleBadge.setTextColor(Color.WHITE);
            // badge rouge (défini dans badge_role_bg.xml)

            // Section admin visible
            sectionAdminOnly.setVisibility(View.VISIBLE);

            // Sous-titres complets
            tvSubVoitures.setText("Ajouter, modifier, supprimer");
            tvSubClients.setText("Ajouter, modifier, supprimer");
            tvSubPaiements.setText("Encaisser, supprimer, historique global");

        } else {
            // Employé
            tvWelcome.setText("Bonjour, " + username + " 👔");
            tvRoleBadge.setText("  EMPLOYÉ  ");
            tvRoleBadge.setTextColor(Color.WHITE);
            tvRoleBadge.setBackgroundResource(R.drawable.badge_employe_bg);

            // Section admin masquée
            sectionAdminOnly.setVisibility(View.GONE);

            // Sous-titres restreints
            tvSubVoitures.setText("Ajouter, modifier (pas supprimer)");
            tvSubClients.setText("Ajouter, modifier (pas supprimer)");
            tvSubPaiements.setText("Consulter uniquement — pas de suppression");
        }
    }

    private void loadStats() {
        tvNbVoitures.setText(String.valueOf(voitureDAO.countAll()));
        tvNbDispo.setText(String.valueOf(voitureDAO.countDisponibles()));
        tvNbClients.setText(String.valueOf(clientDAO.countAll()));
        tvNbReservations.setText(String.valueOf(reservationDAO.countEnCours()));
    }

    private void setupCards() {
        // Voitures
        CardView cardVoitures = findViewById(R.id.card_voitures);
        cardVoitures.setOnClickListener(v ->
                startActivity(new Intent(this, VoitureListActivity.class)));

        // Clients
        CardView cardClients = findViewById(R.id.card_clients);
        cardClients.setOnClickListener(v ->
                startActivity(new Intent(this, ClientListActivity.class)));

        // Réservations
        CardView cardReservations = findViewById(R.id.card_reservations);
        cardReservations.setOnClickListener(v ->
                startActivity(new Intent(this, ReservationListActivity.class)));

        // Paiements — admin voit tout, employé consultation seule
        CardView cardPaiements = findViewById(R.id.card_paiements);
        cardPaiements.setOnClickListener(v ->
                startActivity(new Intent(this, PaiementListActivity.class)));

        // Géolocalisation
        CardView cardGeo = findViewById(R.id.card_geolocalisation);
        if (cardGeo != null) {
            cardGeo.setOnClickListener(v ->
                    startActivity(new Intent(this, GeolocationActivity.class)));
        }

        // Utilisateurs (admin seul — la card est dans la section admin_only)
        CardView cardUtilisateurs = findViewById(R.id.card_utilisateurs);
        if (cardUtilisateurs != null) {
            cardUtilisateurs.setOnClickListener(v ->
                    // Pour l'instant ouvrir la liste avec un message — à implémenter
                    new AlertDialog.Builder(this)
                            .setTitle("Gestion des utilisateurs")
                            .setMessage("Cette section permet de créer des comptes admin et employé.\n\n" +
                                    "Comptes existants :\n" +
                                    "• admin / admin123 (Admin)\n" +
                                    "• employe1 / employe123 (Employé)\n\n" +
                                    "Les clients créent leur compte via l'écran d'inscription.")
                            .setPositiveButton("OK", null)
                            .show());
        }

        // Stats admin
        CardView cardStats = findViewById(R.id.card_stats_global);
        if (cardStats != null) {
            cardStats.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("Droits exclusifs Admin")
                            .setMessage("En tant qu'administrateur, vous pouvez :\n\n" +
                                    "✅ Supprimer des voitures\n" +
                                    "✅ Supprimer des clients\n" +
                                    "✅ Supprimer des paiements\n" +
                                    "✅ Gérer les comptes utilisateurs\n\n" +
                                    "L'employé peut ajouter et modifier, mais pas supprimer.")
                            .setPositiveButton("OK", null)
                            .show());
        }
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
