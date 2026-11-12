package com.agence.locationvoiture.models;

/**
 * Modèle représentant un utilisateur de l'application.
 * Rôles possibles : "admin", "employe", "client"
 */
public class Utilisateur {
    private int id;
    private String username;
    private String password; // Stocké hashé (MD5)
    private String role;

    public Utilisateur() {}

    public Utilisateur(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public Utilisateur(int id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isAdmin() { return "admin".equals(role); }
    public boolean isEmploye() { return "employe".equals(role); }
    public boolean isClient() { return "client".equals(role); }

    @Override
    public String toString() {
        return "Utilisateur{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }
}
