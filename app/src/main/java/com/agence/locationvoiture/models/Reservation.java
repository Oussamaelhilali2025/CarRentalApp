package com.agence.locationvoiture.models;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Modèle représentant une réservation de véhicule.
 * Statuts : "en_cours", "terminee", "annulee"
 */
public class Reservation {
    private int id;
    private int clientId;
    private int voitureId;
    private String dateDebut; // Format ISO : yyyy-MM-dd
    private String dateFin;   // Format ISO : yyyy-MM-dd
    private double prixTotal;
    private String statut;

    // Champs joints (pour affichage) — non stockés en base
    private String clientNom;
    private String voitureNom;

    public static final String STATUT_EN_COURS = "en_cours";
    public static final String STATUT_TERMINEE  = "terminee";
    public static final String STATUT_ANNULEE   = "annulee";

    public Reservation() {}

    public Reservation(int clientId, int voitureId, String dateDebut, String dateFin,
                       double prixTotal, String statut) {
        this.clientId  = clientId;
        this.voitureId = voitureId;
        this.dateDebut = dateDebut;
        this.dateFin   = dateFin;
        this.prixTotal = prixTotal;
        this.statut    = statut;
    }

    public Reservation(int id, int clientId, int voitureId, String dateDebut, String dateFin,
                       double prixTotal, String statut) {
        this.id        = id;
        this.clientId  = clientId;
        this.voitureId = voitureId;
        this.dateDebut = dateDebut;
        this.dateFin   = dateFin;
        this.prixTotal = prixTotal;
        this.statut    = statut;
    }

    /**
     * Calcule le nombre de jours entre dateDebut et dateFin.
     * Retourne 0 si les dates sont invalides.
     */
    public long getNombreJours() {
        try {
            LocalDate debut = LocalDate.parse(dateDebut);
            LocalDate fin   = LocalDate.parse(dateFin);
            return ChronoUnit.DAYS.between(debut, fin);
        } catch (Exception e) {
            return 0;
        }
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public int getVoitureId() { return voitureId; }
    public void setVoitureId(int voitureId) { this.voitureId = voitureId; }

    public String getDateDebut() { return dateDebut; }
    public void setDateDebut(String dateDebut) { this.dateDebut = dateDebut; }

    public String getDateFin() { return dateFin; }
    public void setDateFin(String dateFin) { this.dateFin = dateFin; }

    public double getPrixTotal() { return prixTotal; }
    public void setPrixTotal(double prixTotal) { this.prixTotal = prixTotal; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getVoitureNom() { return voitureNom; }
    public void setVoitureNom(String voitureNom) { this.voitureNom = voitureNom; }

    @Override
    public String toString() {
        return "Réservation #" + id + " | " + clientNom + " → " + voitureNom +
               " | " + dateDebut + " au " + dateFin + " | " + prixTotal + " MAD";
    }
}
