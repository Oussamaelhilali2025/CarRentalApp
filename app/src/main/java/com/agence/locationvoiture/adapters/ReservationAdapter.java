package com.agence.locationvoiture.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.models.Reservation;
import com.agence.locationvoiture.utils.DateUtils;

import java.util.List;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder> {

    public interface OnReservationClickListener {
        void onDetailClick(Reservation reservation);
        void onAnnulerClick(Reservation reservation);
        void onTerminerClick(Reservation reservation);
    }

    private List<Reservation> reservations;
    private final OnReservationClickListener listener;
    private final boolean isClientMode; // true = masquer "Terminer"

    /** Constructeur standard (admin/employé) */
    public ReservationAdapter(List<Reservation> reservations, OnReservationClickListener listener) {
        this(reservations, listener, false);
    }

    /** Constructeur avec mode client — masque le bouton "Terminer" */
    public ReservationAdapter(List<Reservation> reservations,
                              OnReservationClickListener listener,
                              boolean isClientMode) {
        this.reservations  = reservations;
        this.listener      = listener;
        this.isClientMode  = isClientMode;
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ReservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        Reservation r = reservations.get(position);

        holder.tvId.setText("Réservation #" + r.getId());
        holder.tvClient.setText("Client : " + r.getClientNom());
        holder.tvVoiture.setText("Voiture : " + r.getVoitureNom());
        holder.tvDates.setText(
                DateUtils.toDisplay(r.getDateDebut()) + " → " + DateUtils.toDisplay(r.getDateFin()));
        holder.tvPrix.setText(String.format("%.2f MAD", r.getPrixTotal()));
        holder.tvStatut.setText(r.getStatut().replace("_", " ").toUpperCase());

        // Couleur selon statut
        int color;
        switch (r.getStatut()) {
            case Reservation.STATUT_EN_COURS:
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.blue);
                break;
            case Reservation.STATUT_TERMINEE:
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.green);
                break;
            default:
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.red);
                break;
        }
        holder.tvStatut.setTextColor(color);

        boolean isEnCours = Reservation.STATUT_EN_COURS.equals(r.getStatut());

        // Bouton Annuler : visible si en cours (tous les rôles)
        holder.btnAnnuler.setVisibility(isEnCours ? View.VISIBLE : View.GONE);

        // Bouton Terminer : visible si en cours ET pas en mode client
        holder.btnTerminer.setVisibility(isEnCours && !isClientMode ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onDetailClick(r));
        holder.btnAnnuler.setOnClickListener(v -> listener.onAnnulerClick(r));
        holder.btnTerminer.setOnClickListener(v -> listener.onTerminerClick(r));
    }

    @Override
    public int getItemCount() { return reservations.size(); }

    public void updateList(List<Reservation> newList) {
        this.reservations = newList;
        notifyDataSetChanged();
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvClient, tvVoiture, tvDates, tvPrix, tvStatut;
        Button btnAnnuler, btnTerminer;

        ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId        = itemView.findViewById(R.id.tv_id);
            tvClient    = itemView.findViewById(R.id.tv_client);
            tvVoiture   = itemView.findViewById(R.id.tv_voiture);
            tvDates     = itemView.findViewById(R.id.tv_dates);
            tvPrix      = itemView.findViewById(R.id.tv_prix);
            tvStatut    = itemView.findViewById(R.id.tv_statut);
            btnAnnuler  = itemView.findViewById(R.id.btn_annuler);
            btnTerminer = itemView.findViewById(R.id.btn_terminer);
        }
    }
}
