package com.agence.locationvoiture.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.dao.ClientDAO;
import com.agence.locationvoiture.dao.ReservationDAO;
import com.agence.locationvoiture.dao.VoitureDAO;
import com.agence.locationvoiture.models.Client;
import com.agence.locationvoiture.models.Reservation;
import com.agence.locationvoiture.models.Voiture;
import com.agence.locationvoiture.services.NotificationService;
import com.agence.locationvoiture.utils.DateUtils;
import com.agence.locationvoiture.utils.SessionManager;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Formulaire de création d'une réservation.
 *
 * COMPORTEMENT PAR RÔLE :
 * - Admin / Employé : spinner client = TOUS les clients
 * - Client connecté : spinner client = verrouillé sur SA fiche client uniquement
 *                     (on retrouve sa fiche via utilisateur_id)
 */
public class ReservationFormActivity extends AppCompatActivity {

    private Spinner  spinnerClient, spinnerVoiture;
    private TextView tvDateDebut, tvDateFin, tvPrixCalcule, tvClientLabel;
    private Button   btnDateDebut, btnDateFin, btnSave;

    private ClientDAO      clientDAO;
    private VoitureDAO     voitureDAO;
    private ReservationDAO reservationDAO;
    private NotificationService notifService;
    private SessionManager sessionManager;

    private List<Client>  clients;
    private List<Voiture> voitures;
    private String selectedDateDebut = "";
    private String selectedDateFin   = "";

    // Fiche client du compte connecté (null si admin/employé)
    private Client monProfil = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_form);

        clientDAO      = new ClientDAO(this);
        voitureDAO     = new VoitureDAO(this);
        reservationDAO = new ReservationDAO(this);
        notifService   = new NotificationService(this);
        sessionManager = new SessionManager(this);

        // Si client connecté → récupérer sa fiche
        if (sessionManager.isClient()) {
            monProfil = clientDAO.getClientByUserId(sessionManager.getUserId());
            if (monProfil == null) {
                Toast.makeText(this,
                        "Votre profil client n'est pas configuré.\nContactez l'agence.",
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Nouvelle Réservation");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spinnerClient  = findViewById(R.id.spinner_client);
        spinnerVoiture = findViewById(R.id.spinner_voiture);
        tvDateDebut    = findViewById(R.id.tv_date_debut);
        tvDateFin      = findViewById(R.id.tv_date_fin);
        tvPrixCalcule  = findViewById(R.id.tv_prix_calcule);
        tvClientLabel  = findViewById(R.id.tv_client_label);
        btnDateDebut   = findViewById(R.id.btn_date_debut);
        btnDateFin     = findViewById(R.id.btn_date_fin);
        btnSave        = findViewById(R.id.btn_save);

        loadSpinners();

        btnDateDebut.setOnClickListener(v -> showDatePicker(true));
        btnDateFin.setOnClickListener(v -> showDatePicker(false));

        spinnerVoiture.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { calculerPrix(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnSave.setOnClickListener(v -> sauvegarder());
    }

    private void loadSpinners() {
        // ── Spinner Client ──
        if (sessionManager.isClient()) {
            // Client : son nom affiché en lecture seule, spinner désactivé
            clients = Collections.singletonList(monProfil);
            String[] names = {monProfil.getNomComplet()};
            spinnerClient.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, names));
            spinnerClient.setEnabled(false);

            // Changer le label pour montrer que c'est verrouillé
            if (tvClientLabel != null) {
                tvClientLabel.setText("Client (votre compte)");
            }
        } else {
            // Admin / Employé : tous les clients
            clients = clientDAO.getAllClients();
            if (clients.isEmpty()) {
                Toast.makeText(this, "Aucun client. Ajoutez d'abord un client.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            String[] names = clients.stream()
                    .map(c -> c.getNomComplet() + " — " + c.getTelephone())
                    .toArray(String[]::new);
            spinnerClient.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, names));
        }

        // ── Spinner Voiture (disponibles uniquement) ──
        voitures = voitureDAO.getVoituresDisponibles();
        if (voitures.isEmpty()) {
            Toast.makeText(this, "Aucune voiture disponible actuellement.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String[] voitureNames = voitures.stream()
                .map(v -> v.getMarque() + " " + v.getModele() +
                          " (" + v.getAnnee() + ") — " + (int)v.getPrixJour() + " MAD/j")
                .toArray(String[]::new);
        spinnerVoiture.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, voitureNames));
    }

    private void showDatePicker(boolean isDebut) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String date = String.format("%04d-%02d-%02d", year, month + 1, day);
                    if (isDebut) {
                        selectedDateDebut = date;
                        tvDateDebut.setText("📅 " + DateUtils.toDisplay(date));
                    } else {
                        selectedDateFin = date;
                        tvDateFin.setText("📅 " + DateUtils.toDisplay(date));
                    }
                    calculerPrix();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void calculerPrix() {
        if (selectedDateDebut.isEmpty() || selectedDateFin.isEmpty()) return;
        if (voitures == null || voitures.isEmpty()) return;

        int pos = spinnerVoiture.getSelectedItemPosition();
        if (pos < 0 || pos >= voitures.size()) return;

        Voiture voiture = voitures.get(pos);
        long jours = DateUtils.daysBetween(selectedDateDebut, selectedDateFin);
        double prix = DateUtils.calculerPrixTotal(selectedDateDebut, selectedDateFin, voiture.getPrixJour());

        if (jours > 0) {
            tvPrixCalcule.setText(String.format(
                    "💰 Prix total : %.2f MAD\n(%d jours × %.0f MAD/jour)",
                    prix, jours, voiture.getPrixJour()));
            tvPrixCalcule.setTextColor(0xFF2E7D32);
        } else {
            tvPrixCalcule.setText("⚠ La date de fin doit être après la date de début.");
            tvPrixCalcule.setTextColor(0xFFC62828);
        }
    }

    private void sauvegarder() {
        if (selectedDateDebut.isEmpty()) {
            Toast.makeText(this, "Choisissez la date de début", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDateFin.isEmpty()) {
            Toast.makeText(this, "Choisissez la date de fin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!DateUtils.isValidRange(selectedDateDebut, selectedDateFin)) {
            Toast.makeText(this, "La date de fin doit être après la date de début", Toast.LENGTH_SHORT).show();
            return;
        }

        Client  client  = clients.get(spinnerClient.getSelectedItemPosition());
        Voiture voiture = voitures.get(spinnerVoiture.getSelectedItemPosition());

        if (reservationDAO.voitureDejaReservee(voiture.getId(), selectedDateDebut, selectedDateFin, -1)) {
            Toast.makeText(this,
                    voiture.getMarque() + " " + voiture.getModele() +
                    " est déjà réservée sur cette période.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        double prixTotal = DateUtils.calculerPrixTotal(selectedDateDebut, selectedDateFin, voiture.getPrixJour());

        Reservation reservation = new Reservation(
                client.getId(), voiture.getId(),
                selectedDateDebut, selectedDateFin,
                prixTotal, Reservation.STATUT_EN_COURS);

        long id = reservationDAO.addReservation(reservation);
        if (id > 0) {
            Toast.makeText(this, "✅ Réservation créée avec succès !", Toast.LENGTH_SHORT).show();

            // Notification
            notifService.notifierReservationCree((int) id,
                    client.getNomComplet(),
                    voiture.getNomComplet(),
                    DateUtils.toDisplay(selectedDateDebut),
                    DateUtils.toDisplay(selectedDateFin));
            finish();
        } else {
            Toast.makeText(this, "Erreur lors de la création", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
