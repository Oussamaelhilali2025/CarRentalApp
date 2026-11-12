package com.agence.locationvoiture.models;

/**
 * Modèle représentant un client de l'agence.
 * Peut être lié à un compte utilisateur via utilisateurId.
 * Si utilisateurId == 0, le client a été créé manuellement par l'admin (sans compte).
 */
public class Client {
    private int    id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private int    utilisateurId; // 0 = pas de compte lié

    public Client() {}

    public Client(String nom, String prenom, String email, String telephone) {
        this.nom       = nom;
        this.prenom    = prenom;
        this.email     = email;
        this.telephone = telephone;
    }

    public Client(int id, String nom, String prenom, String email, String telephone) {
        this.id        = id;
        this.nom       = nom;
        this.prenom    = prenom;
        this.email     = email;
        this.telephone = telephone;
    }

    public Client(int id, String nom, String prenom, String email, String telephone, int utilisateurId) {
        this.id             = id;
        this.nom            = nom;
        this.prenom         = prenom;
        this.email          = email;
        this.telephone      = telephone;
        this.utilisateurId  = utilisateurId;
    }

    // Getters & Setters
    public int    getId()           { return id; }
    public void   setId(int id)     { this.id = id; }

    public String getNom()          { return nom; }
    public void   setNom(String v)  { this.nom = v; }

    public String getPrenom()           { return prenom; }
    public void   setPrenom(String v)   { this.prenom = v; }

    public String getEmail()            { return email; }
    public void   setEmail(String v)    { this.email = v; }

    public String getTelephone()        { return telephone; }
    public void   setTelephone(String v){ this.telephone = v; }

    public int    getUtilisateurId()         { return utilisateurId; }
    public void   setUtilisateurId(int v)    { this.utilisateurId = v; }

    public boolean hasCompte() { return utilisateurId > 0; }

    public String getNomComplet() { return prenom + " " + nom; }

    @Override
    public String toString() { return getNomComplet() + " <" + email + ">"; }
}
