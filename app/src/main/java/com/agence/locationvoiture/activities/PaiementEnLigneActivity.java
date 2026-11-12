package com.agence.locationvoiture.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.dao.ClientDAO;
import com.agence.locationvoiture.dao.PaiementDAO;
import com.agence.locationvoiture.dao.ReservationDAO;
import com.agence.locationvoiture.models.Client;
import com.agence.locationvoiture.models.Paiement;
import com.agence.locationvoiture.models.Reservation;
import com.agence.locationvoiture.services.NotificationService;
import com.agence.locationvoiture.utils.DateUtils;
import com.agence.locationvoiture.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Paiement en ligne.
 *
 * Peut être appelé de 2 façons :
 * 1. Avec  reservation_id dans l'Intent → payer directement cette réservation
 * 2. Sans  reservation_id              → afficher un spinner pour choisir parmi
 *                                        les réservations avec solde impayé
 *                                        (utilisé depuis le dashboard client)
 */
public class PaiementEnLigneActivity extends AppCompatActivity {

    private TextView tvInfoReservation, tvMontantDue, tvResultat, tvChoixLabel;
    private Spinner  spinnerReservations;
    private RadioGroup rgMethode;
    private RadioButton rbCarte, rbVirement, rbPaypal;
    private LinearLayout layoutCarte, layoutVirement, layoutChoixReservation;
    private EditText etNumeroCarte, etNomPorteur, etExpiry, etCvv;
    private EditText etIban, etNomBanque;
    private Button   btnPayer;
    private ProgressBar progressBar;
    private CardView    cardResultat, cardInfo;

    private int    reservationId = -1;
    private double montantDue    = 0;

    private ReservationDAO reservationDAO;
    private PaiementDAO    paiementDAO;
    private ClientDAO      clientDAO;
    private NotificationService notifService;
    private SessionManager sessionManager;

    // Pour le spinner multi-réservations
    private List<Reservation> reservationsImpayees = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paiement_en_ligne);

        reservationDAO = new ReservationDAO(this);
        paiementDAO    = new PaiementDAO(this);
        clientDAO      = new ClientDAO(this);
        notifService   = new NotificationService(this);
        sessionManager = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("💳 Paiement en ligne");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();

        reservationId = getIntent().getIntExtra("reservation_id", -1);

        if (reservationId != -1) {
            // Cas 1 : réservation spécifique
            layoutChoixReservation.setVisibility(View.GONE);
            loadReservationInfo(reservationId);
        } else {
            // Cas 2 : client vient du dashboard, doit choisir
            layoutChoixReservation.setVisibility(View.VISIBLE);
            cardInfo.setVisibility(View.GONE);
            btnPayer.setEnabled(false);
            loadReservationsImpayees();
        }

        setupMethodeListener();
        btnPayer.setOnClickListener(v -> traiterPaiement());
    }

    private void initViews() {
        tvInfoReservation      = findViewById(R.id.tv_info_reservation);
        tvMontantDue           = findViewById(R.id.tv_montant_due);
        tvResultat             = findViewById(R.id.tv_resultat);
        tvChoixLabel           = findViewById(R.id.tv_choix_label);
        spinnerReservations    = findViewById(R.id.spinner_reservations);
        layoutChoixReservation = findViewById(R.id.layout_choix_reservation);
        rgMethode              = findViewById(R.id.rg_methode);
        rbCarte                = findViewById(R.id.rb_carte);
        rbVirement             = findViewById(R.id.rb_virement);
        rbPaypal               = findViewById(R.id.rb_paypal);
        layoutCarte            = findViewById(R.id.layout_carte);
        layoutVirement         = findViewById(R.id.layout_virement);
        etNumeroCarte          = findViewById(R.id.et_numero_carte);
        etNomPorteur           = findViewById(R.id.et_nom_porteur);
        etExpiry               = findViewById(R.id.et_expiry);
        etCvv                  = findViewById(R.id.et_cvv);
        etIban                 = findViewById(R.id.et_iban);
        etNomBanque            = findViewById(R.id.et_nom_banque);
        btnPayer               = findViewById(R.id.btn_payer);
        progressBar            = findViewById(R.id.progress_bar);
        cardResultat           = findViewById(R.id.card_resultat);
        cardInfo               = findViewById(R.id.card_info);
    }

    /** Charge les réservations avec solde impayé pour le client connecté */
    private void loadReservationsImpayees() {
        Client profil = sessionManager.isClient()
                ? clientDAO.getClientByUserId(sessionManager.getUserId())
                : null;

        List<Reservation> toutes = (profil != null)
                ? reservationDAO.getReservationsByClient(profil.getId())
                : reservationDAO.getAllReservations();

        reservationsImpayees.clear();
        for (Reservation r : toutes) {
            if (!Reservation.STATUT_ANNULEE.equals(r.getStatut())) {
                double reste = r.getPrixTotal() - paiementDAO.getTotalPaye(r.getId());
                if (reste > 0.01) {
                    reservationsImpayees.add(r);
                }
            }
        }

        if (reservationsImpayees.isEmpty()) {
            tvChoixLabel.setText("✅ Toutes vos réservations sont déjà entièrement payées.");
            spinnerReservations.setVisibility(View.GONE);
            return;
        }

        String[] labels = reservationsImpayees.stream()
                .map(r -> {
                    double reste = r.getPrixTotal() - paiementDAO.getTotalPaye(r.getId());
                    return "#" + r.getId() + " — " + r.getVoitureNom() +
                            " (" + DateUtils.toDisplay(r.getDateDebut()) + ") — " +
                            String.format("%.0f MAD", reste);
                })
                .toArray(String[]::new);

        spinnerReservations.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels));

        spinnerReservations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                Reservation selected = reservationsImpayees.get(pos);
                reservationId = selected.getId();
                loadReservationInfo(reservationId);
                cardInfo.setVisibility(View.VISIBLE);
                btnPayer.setEnabled(true);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Auto-sélect first
        reservationId = reservationsImpayees.get(0).getId();
        loadReservationInfo(reservationId);
        cardInfo.setVisibility(View.VISIBLE);
        btnPayer.setEnabled(true);
    }

    private void loadReservationInfo(int resId) {
        Reservation r = reservationDAO.getReservationById(resId);
        if (r == null) return;

        double totalPaye = paiementDAO.getTotalPaye(resId);
        montantDue = Math.max(0, r.getPrixTotal() - totalPaye);

        tvInfoReservation.setText(String.format(
                "Réservation #%d\n%s\n%s → %s",
                r.getId(),
                r.getVoitureNom(),
                DateUtils.toDisplay(r.getDateDebut()),
                DateUtils.toDisplay(r.getDateFin())));

        tvMontantDue.setText(String.format("Montant à payer : %.2f MAD", montantDue));

        if (montantDue <= 0) {
            btnPayer.setEnabled(false);
            btnPayer.setText("Déjà entièrement payé ✅");
        } else {
            btnPayer.setEnabled(true);
            btnPayer.setText("🔒  Payer " + String.format("%.2f MAD", montantDue));
        }
    }

    private void setupMethodeListener() {
        rgMethode.setOnCheckedChangeListener((group, id) -> {
            layoutCarte.setVisibility(View.GONE);
            layoutVirement.setVisibility(View.GONE);
            if (id == R.id.rb_carte)    layoutCarte.setVisibility(View.VISIBLE);
            if (id == R.id.rb_virement) layoutVirement.setVisibility(View.VISIBLE);
        });
        rbCarte.setChecked(true);
        layoutCarte.setVisibility(View.VISIBLE);
    }

    private void traiterPaiement() {
        if (reservationId == -1) {
            Toast.makeText(this, "Sélectionnez d'abord une réservation", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!validerChamps()) return;

        btnPayer.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        cardResultat.setVisibility(View.GONE);

        String methode = getMethodeSelectionnee();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            progressBar.setVisibility(View.GONE);

            boolean succes = simulerPaiement();
            cardResultat.setVisibility(View.VISIBLE);

            if (succes) {
                String date = DateUtils.today();
                Paiement p = new Paiement(reservationId, montantDue, date);
                long newId = paiementDAO.addPaiement(p);

                if (newId > 0) {
                    tvResultat.setText("✅ Paiement de " +
                            String.format("%.2f MAD", montantDue) +
                            " confirmé !\n\nMéthode : " + methode +
                            "\nRéférence : TXN-" + System.currentTimeMillis());
                    tvResultat.setTextColor(0xFF2E7D32);
                    btnPayer.setText("Paiement effectué ✅");
                    notifService.notifierPaiementEnLigneReussi(reservationId, montantDue, methode);
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 3000);
                } else {
                    afficherErreur("Erreur lors de l'enregistrement du paiement.");
                }
            } else {
                afficherErreur("❌ Paiement refusé.\n\nVérifiez vos informations et réessayez.\n(Test : terminez le numéro par 0000 pour simuler un refus)");
                btnPayer.setEnabled(true);
            }
        }, 2500);
    }

    private boolean validerChamps() {
        int id = rgMethode.getCheckedRadioButtonId();
        if (id == R.id.rb_carte) {
            String num = etNumeroCarte.getText().toString().trim().replace(" ", "");
            if (num.length() < 16) { etNumeroCarte.setError("16 chiffres requis"); return false; }
            if (TextUtils.isEmpty(etNomPorteur.getText())) { etNomPorteur.setError("Nom requis"); return false; }
            if (!etExpiry.getText().toString().matches("(0[1-9]|1[0-2])/\\d{2}")) { etExpiry.setError("Format MM/AA"); return false; }
            if (etCvv.getText().toString().length() < 3) { etCvv.setError("CVV invalide"); return false; }
        } else if (id == R.id.rb_virement) {
            if (etIban.getText().toString().trim().length() < 15) { etIban.setError("IBAN invalide"); return false; }
            if (TextUtils.isEmpty(etNomBanque.getText())) { etNomBanque.setError("Banque requise"); return false; }
        }
        return true;
    }

    private boolean simulerPaiement() {
        if (rbCarte.isChecked()) {
            String num = etNumeroCarte.getText().toString().trim().replace(" ", "");
            return !num.endsWith("0000");
        }
        return true;
    }

    private String getMethodeSelectionnee() {
        int id = rgMethode.getCheckedRadioButtonId();
        if (id == R.id.rb_carte)    return "Carte bancaire";
        if (id == R.id.rb_virement) return "Virement bancaire";
        return "PayPal";
    }

    private void afficherErreur(String msg) {
        tvResultat.setText(msg);
        tvResultat.setTextColor(0xFFC62828);
        cardResultat.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
