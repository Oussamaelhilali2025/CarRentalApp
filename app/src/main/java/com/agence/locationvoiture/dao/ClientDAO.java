package com.agence.locationvoiture.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.agence.locationvoiture.database.DatabaseHelper;
import com.agence.locationvoiture.models.Client;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour les opérations CRUD sur les clients.
 *
 * Gestion des rôles (au niveau DAO) :
 *  - addClient      : admin + employé
 *  - updateClient   : admin + employé
 *  - deleteClient   : admin uniquement (enforced dans l'Activity)
 *  - getClientByUserId : permet de retrouver la fiche d'un client connecté
 */
public class ClientDAO {

    private final DatabaseHelper dbHelper;

    public ClientDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    /** Ajoute un client. Retourne l'ID généré. */
    public long addClient(Client c) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DatabaseHelper.TABLE_CLIENT, null, buildValues(c));
    }

    /** Met à jour un client existant. */
    public int updateClient(Client c) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(DatabaseHelper.TABLE_CLIENT, buildValues(c),
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(c.getId())});
    }

    /** Supprime un client (admin uniquement — vérifié dans l'Activity). */
    public int deleteClient(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_CLIENT,
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    /** Retourne tous les clients, triés par nom. */
    public List<Client> getAllClients() {
        List<Client> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CLIENT,
                null, null, null, null, null,
                DatabaseHelper.COL_NOM + " ASC");
        while (cursor.moveToNext()) list.add(cursorToClient(cursor));
        cursor.close();
        return list;
    }

    /** Retourne un client par son ID. */
    public Client getClientById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CLIENT, null,
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);
        Client c = null;
        if (cursor.moveToFirst()) c = cursorToClient(cursor);
        cursor.close();
        return c;
    }

    /**
     * Retrouve la fiche client liée à un compte utilisateur.
     * Utilisé quand un client se connecte pour obtenir son client_id
     * et ainsi filtrer ses réservations et paiements.
     * @param utilisateurId L'ID de l'utilisateur connecté (sessionManager.getUserId())
     * @return La fiche Client liée, ou null si le client n'a pas de compte lié
     */
    public Client getClientByUserId(int utilisateurId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CLIENT, null,
                DatabaseHelper.COL_UTILISATEUR_ID + "=?",
                new String[]{String.valueOf(utilisateurId)},
                null, null, null);
        Client c = null;
        if (cursor.moveToFirst()) c = cursorToClient(cursor);
        cursor.close();
        return c;
    }

    /** Recherche des clients par nom, prénom ou email. */
    public List<Client> searchClients(String query) {
        List<Client> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String like = "%" + query + "%";
        Cursor cursor = db.query(DatabaseHelper.TABLE_CLIENT, null,
                DatabaseHelper.COL_NOM + " LIKE ? OR " +
                DatabaseHelper.COL_PRENOM + " LIKE ? OR " +
                DatabaseHelper.COL_EMAIL + " LIKE ?",
                new String[]{like, like, like},
                null, null, DatabaseHelper.COL_NOM + " ASC");
        while (cursor.moveToNext()) list.add(cursorToClient(cursor));
        cursor.close();
        return list;
    }

    /** Vérifie si un email est déjà utilisé (lors de la création/modification). */
    public boolean emailExiste(String email, int excludeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CLIENT,
                new String[]{DatabaseHelper.COL_ID},
                DatabaseHelper.COL_EMAIL + "=? AND " + DatabaseHelper.COL_ID + "!=?",
                new String[]{email, String.valueOf(excludeId)},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /** Vérifie si un email existe déjà (pour l'inscription — sans excludeId). */
    public boolean emailExiste(String email) {
        return emailExiste(email, -1);
    }

    /** Nombre total de clients. */
    public int countAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_CLIENT, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    private ContentValues buildValues(Client c) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_NOM,       c.getNom());
        values.put(DatabaseHelper.COL_PRENOM,    c.getPrenom());
        values.put(DatabaseHelper.COL_EMAIL,     c.getEmail());
        values.put(DatabaseHelper.COL_TELEPHONE, c.getTelephone());
        if (c.getUtilisateurId() > 0) {
            values.put(DatabaseHelper.COL_UTILISATEUR_ID, c.getUtilisateurId());
        }
        return values;
    }

    private Client cursorToClient(Cursor cursor) {
        int utilisateurIdCol = cursor.getColumnIndex(DatabaseHelper.COL_UTILISATEUR_ID);
        int utilisateurId = (utilisateurIdCol >= 0 && !cursor.isNull(utilisateurIdCol))
                ? cursor.getInt(utilisateurIdCol) : 0;

        return new Client(
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOM)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRENOM)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EMAIL)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TELEPHONE)),
                utilisateurId
        );
    }
}
