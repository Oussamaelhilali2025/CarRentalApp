package com.agence.locationvoiture.models;

/**
 * Modèle représentant un véhicule du parc automobile de l'agence.
 */
public class Voiture {
    private int id;
    private String marque;
    private String modele;
    private int annee;
    private double prixJour;
    private boolean disponible;
    private String imagePath; // Chemin local vers la photo (optionnel)

    public Voiture() {}

    public Voiture(String marque, String modele, int annee, double prixJour, boolean disponible) {
        this.marque = marque;
        this.modele = modele;
        this.annee = annee;
        this.prixJour = prixJour;
        this.disponible = disponible;
    }

    public Voiture(int id, String marque, String modele, int annee, double prixJour, boolean disponible, String imagePath) {
        this.id = id;
        this.marque = marque;
        this.modele = modele;
        this.annee = annee;
        this.prixJour = prixJour;
        this.disponible = disponible;
        this.imagePath = imagePath;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }

    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public double getPrixJour() { return prixJour; }
    public void setPrixJour(double prixJour) { this.prixJour = prixJour; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    /** Retourne le nom complet du véhicule : Marque Modèle (Année) */
    public String getNomComplet() {
        return marque + " " + modele + " (" + annee + ")";
    }

    @Override
    public String toString() {
        return getNomComplet() + " - " + prixJour + " MAD/jour";
    }
}
