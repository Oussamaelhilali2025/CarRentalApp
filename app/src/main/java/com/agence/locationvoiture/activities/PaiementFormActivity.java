package com.agence.locationvoiture.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.dao.PaiementDAO;
import com.agence.locationvoiture.dao.ReservationDAO;
import com.agence.locationvoiture.models.Paiement;
import com.agence.locationvoiture.models.Reservation;
import com.agence.locationvoiture.utils.DateUtils;
import com.agence.locationvoiture.utils.SessionManager;
import com.agence.locationvoiture.services.NotificationService;

import java.util.Calendar;

public class PaiementFormActivity extends AppCompatActivity {

    private EditText etMontant;
    private TextView tvDatePaiement, tvInfoReservation;
    private Button btnDatePaiement, btnSave;

    private PaiementDAO paiementDAO;
    private ReservationDAO reservationDAO;
    private int reservationId;
    private String selectedDate = "";
    private NotificationService notifService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paiement_form);

        paiementDAO    = new PaiementDAO(this);
        reservationDAO = new ReservationDAO(this);
        reservationId  = getIntent().getIntExtra("reservation_id", -1);
        notifService   = new NotificationService(this);

        // Seuls admin et employé enregistrent un paiement espèces/chèque
        // Les clients utilisent PaiementEnLigneActivity
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isClient()) {
            Toast.makeText(this, "Utilisez le paiement en ligne", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Enregistrer un Paiement");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etMontant        = findViewById(R.id.et_montant);
        tvDatePaiement   = findViewById(R.id.tv_date_paiement);
        tvInfoReservation = findViewById(R.id.tv_info_reservation);
        btnDatePaiement  = findViewById(R.id.btn_date_paiement);
        btnSave          = findViewById(R.id.btn_save);

        // Date par défaut = aujourd'hui
        selectedDate = DateUtils.today();
        tvDatePaiement.setText(DateUtils.toDisplay(selectedDate));

        loadInfoReservation();

        btnDatePaiement.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> sauvegarder());
    }

    private void loadInfoReservation() {
        Reservation r = reservationDAO.getReservationById(reservationId);
        if (r != null) {
            double totalPaye = paiementDAO.getTotalPaye(reservationId);
            double reste = r.getPrixTotal() - totalPaye;
            tvInfoReservation.setText(String.format(
                    "Réservation #%d\nClient : %s\nVoiture : %s\nTotal : %.2f MAD\nDéjà payé : %.2f MAD\nReste : %.2f MAD",
                    r.getId(), r.getClientNom(), r.getVoitureNom(),
                    r.getPrixTotal(), totalPaye, Math.max(0, reste)));
            // Pré-remplir avec le reste à payer
            etMontant.setText(String.format("%.2f", Math.max(0, reste)));
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            tvDatePaiement.setText(DateUtils.toDisplay(selectedDate));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void sauvegarder() {
        String montantStr = etMontant.getText().toString().trim();
        if (TextUtils.isEmpty(montantStr)) { etMontant.setError("Montant requis"); return; }

        double montant;
        try {
            montant = Double.parseDouble(montantStr);
            if (montant <= 0) { etMontant.setError("Le montant doit être positif"); return; }
        } catch (NumberFormatException e) {
            etMontant.setError("Montant invalide"); return;
        }

        Paiement paiement = new Paiement(reservationId, montant, selectedDate);
        long id = paiementDAO.addPaiement(paiement);
        if (id > 0) {
            double totalPaye2 = paiementDAO.getTotalPaye(reservationId);
            Reservation r2 = reservationDAO.getReservationById(reservationId);
            double reste2 = r2 != null ? r2.getPrixTotal() - totalPaye2 : 0;
            notifService.notifierPaiementEnregistre(reservationId, montant, reste2);
            Toast.makeText(this, "Paiement enregistré", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
