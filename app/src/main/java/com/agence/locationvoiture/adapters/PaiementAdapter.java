package com.agence.locationvoiture.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.models.Paiement;
import com.agence.locationvoiture.utils.DateUtils;

import java.util.List;

public class PaiementAdapter extends RecyclerView.Adapter<PaiementAdapter.PaiementViewHolder> {

    public interface OnPaiementDeleteListener {
        void onDeleteClick(Paiement paiement);
    }

    private final List<Paiement> paiements;
    private final OnPaiementDeleteListener listener;

    public PaiementAdapter(List<Paiement> paiements, OnPaiementDeleteListener listener) {
        this.paiements = paiements;
        this.listener  = listener;
    }

    @NonNull
    @Override
    public PaiementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paiement, parent, false);
        return new PaiementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaiementViewHolder holder, int position) {
        Paiement p = paiements.get(position);
        holder.tvMontant.setText(String.format("%.2f MAD", p.getMontant()));
        holder.tvDate.setText(DateUtils.toDisplay(p.getDatePaiement()));
        holder.tvReservation.setText("Réservation #" + p.getReservationId());

        if (listener != null) {
            holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(p));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return paiements.size(); }

    static class PaiementViewHolder extends RecyclerView.ViewHolder {
        TextView tvMontant, tvDate, tvReservation;
        ImageButton btnDelete;

        PaiementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMontant     = itemView.findViewById(R.id.tv_montant);
            tvDate        = itemView.findViewById(R.id.tv_date);
            tvReservation = itemView.findViewById(R.id.tv_reservation);
            btnDelete     = itemView.findViewById(R.id.btn_delete);
        }
    }
}
