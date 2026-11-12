package com.agence.locationvoiture.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestionnaire de session utilisateur via SharedPreferences.
 * Stocke l'identifiant, le nom et le rôle de l'utilisateur connecté.
 */
public class SessionManager {

    private static final String PREF_NAME   = "CarRentalSession";
    private static final String KEY_ID       = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE     = "role";
    private static final String KEY_LOGGED   = "isLoggedIn";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /** Crée une session pour l'utilisateur connecté */
    public void createSession(int userId, String username, String role) {
        editor.putInt(KEY_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_ROLE, role);
        editor.putBoolean(KEY_LOGGED, true);
        editor.apply();
    }

    /** Vérifie si une session est active */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED, false);
    }

    public int getUserId()       { return prefs.getInt(KEY_ID, -1); }
    public String getUsername()  { return prefs.getString(KEY_USERNAME, ""); }
    public String getRole()      { return prefs.getString(KEY_ROLE, "client"); }

    public boolean isAdmin()   { return "admin".equals(getRole()); }
    public boolean isEmploye() { return "employe".equals(getRole()); }
    public boolean isClient()  { return "client".equals(getRole()); }

    /** Détruit la session (déconnexion) */
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
