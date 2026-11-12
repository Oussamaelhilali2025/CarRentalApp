package com.agence.locationvoiture.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.models.Client;

import java.util.List;

public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.ClientViewHolder> {

    public interface OnClientClickListener {
        void onEditClick(Client client);
        void onDeleteClick(Client client);
        void onViewReservations(Client client);
    }

    private List<Client> clients;
    private final OnClientClickListener listener;

    public ClientAdapter(List<Client> clients, OnClientClickListener listener) {
        this.clients  = clients;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_client, parent, false);
        return new ClientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClientViewHolder holder, int position) {
        Client c = clients.get(position);
        holder.tvNom.setText(c.getNomComplet());
        holder.tvEmail.setText(c.getEmail());
        holder.tvTelephone.setText(c.getTelephone());

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(c));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(c));
        holder.btnReservations.setOnClickListener(v -> listener.onViewReservations(c));
    }

    @Override
    public int getItemCount() { return clients.size(); }

    public void updateList(List<Client> newList) {
        this.clients = newList;
        notifyDataSetChanged();
    }

    static class ClientViewHolder extends RecyclerView.ViewHolder {
        TextView tvNom, tvEmail, tvTelephone;
        ImageButton btnEdit, btnDelete, btnReservations;

        ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNom          = itemView.findViewById(R.id.tv_nom);
            tvEmail        = itemView.findViewById(R.id.tv_email);
            tvTelephone    = itemView.findViewById(R.id.tv_telephone);
            btnEdit        = itemView.findViewById(R.id.btn_edit);
            btnDelete      = itemView.findViewById(R.id.btn_delete);
            btnReservations = itemView.findViewById(R.id.btn_reservations);
        }
    }
}
