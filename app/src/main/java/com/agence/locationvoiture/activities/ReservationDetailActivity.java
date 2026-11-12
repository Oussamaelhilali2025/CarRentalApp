package com.agence.locationvoiture.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.adapters.PaiementAdapter;
import com.agence.locationvoiture.dao.PaiementDAO;
import com.agence.locationvoiture.dao.ReservationDAO;
import com.agence.locationvoiture.models.Paiement;
import com.agence.locationvoiture.models.Reservation;
import com.agence.locationvoiture.services.NotificationService;
import com.agence.locationvoiture.utils.DateUtils;

import java.util.List;

/**
 * Détails complets d'une réservation + paiements + boutons paiement classique/en ligne.
 */
public class ReservationDetailActivity extends AppCompatActivity {

    private TextView tvClient, tvVoiture, tvDateDebut, tvDateFin, tvDuree,
            tvPrixTotal, tvStatut, tvTotalPaye, tvReste;
    private RecyclerView recyclerPaiements;
    private Button btnAddPaiement, btnPayerEnLigne;

    private ReservationDAO reservationDAO;
    private PaiementDAO paiementDAO;
    private NotificationService notifService;
    private int reservationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_detail);

        reservationDAO = new ReservationDAO(this);
        paiementDAO    = new PaiementDAO(this);
        notifService   = new NotificationService(this);
        reservationId  = getIntent().getIntExtra("reservation_id", -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Détail Réservation #" + reservationId);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvClient      = findViewById(R.id.tv_client);
        tvVoiture     = findViewById(R.id.tv_voiture);
        tvDateDebut   = findViewById(R.id.tv_date_debut);
        tvDateFin     = findViewById(R.id.tv_date_fin);
        tvDuree       = findViewById(R.id.tv_duree);
        tvPrixTotal   = findViewById(R.id.tv_prix_total);
        tvStatut      = findViewById(R.id.tv_statut);
        tvTotalPaye   = findViewById(R.id.tv_total_paye);
        tvReste       = findViewById(R.id.tv_reste);
        recyclerPaiements = findViewById(R.id.recycler_paiements);
        btnAddPaiement    = findViewById(R.id.btn_add_paiement);
        btnPayerEnLigne   = findViewById(R.id.btn_payer_en_ligne);

        recyclerPaiements.setLayoutManager(new LinearLayoutManager(this));

        btnAddPaiement.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaiementFormActivity.class);
            intent.putExtra("reservation_id", reservationId);
            startActivity(intent);
        });

        btnPayerEnLigne.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaiementEnLigneActivity.class);
            intent.putExtra("reservation_id", reservationId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        Reservation r = reservationDAO.getReservationById(reservationId);
        if (r == null) { finish(); return; }

        tvClient.setText(r.getClientNom());
        tvVoiture.setText(r.getVoitureNom());
        tvDateDebut.setText(DateUtils.toDisplay(r.getDateDebut()));
        tvDateFin.setText(DateUtils.toDisplay(r.getDateFin()));
        tvDuree.setText(r.getNombreJours() + " jour(s)");
        tvPrixTotal.setText(String.format("%.2f MAD", r.getPrixTotal()));
        tvStatut.setText(r.getStatut().replace("_", " ").toUpperCase());

        List<Paiement> paiements = paiementDAO.getPaiementsByReservation(reservationId);
        double totalPaye = paiementDAO.getTotalPaye(reservationId);
        double reste = r.getPrixTotal() - totalPaye;

        tvTotalPaye.setText(String.format("%.2f MAD", totalPaye));
        tvReste.setText(String.format("%.2f MAD", Math.max(0, reste)));

        PaiementAdapter pAdapter = new PaiementAdapter(paiements, p -> {
            paiementDAO.deletePaiement(p.getId());
            Toast.makeText(this, "Paiement supprimé", Toast.LENGTH_SHORT).show();
            loadData();
        });
        recyclerPaiements.setAdapter(pAdapter);

        boolean annulee = Reservation.STATUT_ANNULEE.equals(r.getStatut());
        boolean payee   = reste <= 0;

        if (annulee || payee) {
            btnAddPaiement.setEnabled(false);
            btnPayerEnLigne.setEnabled(false);
            String msg = payee ? "Entièrement payé" : "Réservation annulée";
            btnAddPaiement.setText(msg);
            btnPayerEnLigne.setText(msg);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
