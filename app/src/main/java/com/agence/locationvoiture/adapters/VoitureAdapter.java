package com.agence.locationvoiture.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.models.Voiture;

import java.util.List;

public class VoitureAdapter extends RecyclerView.Adapter<VoitureAdapter.VoitureViewHolder> {

    public interface OnVoitureClickListener {
        void onEditClick(Voiture voiture);
        void onDeleteClick(Voiture voiture);
    }

    private List<Voiture> voitures;
    private final OnVoitureClickListener listener;
    private final boolean readOnly;

    public VoitureAdapter(List<Voiture> voitures, OnVoitureClickListener listener, boolean readOnly) {
        this.voitures  = voitures;
        this.listener  = listener;
        this.readOnly  = readOnly;
    }

    @NonNull
    @Override
    public VoitureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voiture, parent, false);
        return new VoitureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoitureViewHolder holder, int position) {
        Voiture v = voitures.get(position);
        holder.tvNom.setText(v.getNomComplet());
        holder.tvPrix.setText(String.format("%.0f MAD/jour", v.getPrixJour()));
        holder.tvDispo.setText(v.isDisponible() ? "✅ Disponible" : "🔴 Indisponible");
        holder.tvDispo.setTextColor(v.isDisponible()
                ? holder.itemView.getContext().getColor(R.color.green)
                : holder.itemView.getContext().getColor(R.color.red));

        if (readOnly) {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnEdit.setOnClickListener(view -> listener.onEditClick(v));
            holder.btnDelete.setOnClickListener(view -> listener.onDeleteClick(v));
        }
    }

    @Override
    public int getItemCount() { return voitures.size(); }

    public void updateList(List<Voiture> newList) {
        this.voitures = newList;
        notifyDataSetChanged();
    }

    static class VoitureViewHolder extends RecyclerView.ViewHolder {
        TextView tvNom, tvPrix, tvDispo;
        ImageButton btnEdit, btnDelete;

        VoitureViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNom    = itemView.findViewById(R.id.tv_nom);
            tvPrix   = itemView.findViewById(R.id.tv_prix);
            tvDispo  = itemView.findViewById(R.id.tv_dispo);
            btnEdit  = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
