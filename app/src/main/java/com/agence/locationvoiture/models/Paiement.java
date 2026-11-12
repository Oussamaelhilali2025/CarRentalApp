package com.agence.locationvoiture.models;

/**
 * Modèle représentant un paiement lié à une réservation.
 * Champs joints (non stockés en base) pour l'affichage dans les listes.
 */
public class Paiement {
    private int    id;
    private int    reservationId;
    private double montant;
    private String datePaiement;   // Format ISO : yyyy-MM-dd

    // Champs joints pour affichage (peuplés par les requêtes JOIN)
    private String reservationInfo;
    private String clientNom;
    private String voitureNom;

    public Paiement() {}

    public Paiement(int reservationId, double montant, String datePaiement) {
        this.reservationId = reservationId;
        this.montant       = montant;
        this.datePaiement  = datePaiement;
    }

    public Paiement(int id, int reservationId, double montant, String datePaiement) {
        this.id            = id;
        this.reservationId = reservationId;
        this.montant       = montant;
        this.datePaiement  = datePaiement;
    }

    public int    getId()               { return id; }
    public void   setId(int id)         { this.id = id; }

    public int    getReservationId()              { return reservationId; }
    public void   setReservationId(int v)         { this.reservationId = v; }

    public double getMontant()                    { return montant; }
    public void   setMontant(double v)            { this.montant = v; }

    public String getDatePaiement()               { return datePaiement; }
    public void   setDatePaiement(String v)       { this.datePaiement = v; }

    public String getReservationInfo()            { return reservationInfo; }
    public void   setReservationInfo(String v)    { this.reservationInfo = v; }

    public String getClientNom()                  { return clientNom; }
    public void   setClientNom(String v)          { this.clientNom = v; }

    public String getVoitureNom()                 { return voitureNom; }
    public void   setVoitureNom(String v)         { this.voitureNom = v; }

    @Override
    public String toString() {
        return "Paiement #" + id + " | Réservation #" + reservationId +
               " | " + montant + " MAD | " + datePaiement;
    }
}
