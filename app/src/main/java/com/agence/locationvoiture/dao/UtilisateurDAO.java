package com.agence.locationvoiture.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.agence.locationvoiture.database.DatabaseHelper;
import com.agence.locationvoiture.models.Utilisateur;
import com.agence.locationvoiture.utils.HashUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) pour les opérations CRUD sur les utilisateurs.
 * Isole toute la logique SQL des Activities.
 */
public class UtilisateurDAO {

    private final DatabaseHelper dbHelper;

    public UtilisateurDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    /** Authentifie un utilisateur par username + mot de passe (hashé) */
    public Utilisateur authentifier(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String hashedPwd = HashUtils.md5(password);

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_UTILISATEUR,
                null,
                DatabaseHelper.COL_USERNAME + "=? AND " + DatabaseHelper.COL_PASSWORD + "=?",
                new String[]{username, hashedPwd},
                null, null, null
        );

        Utilisateur user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUtilisateur(cursor);
        }
        cursor.close();
        return user;
    }

    /** Ajoute un nouvel utilisateur */
    public long addUtilisateur(Utilisateur u) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USERNAME, u.getUsername());
        values.put(DatabaseHelper.COL_PASSWORD, HashUtils.md5(u.getPassword()));
        values.put(DatabaseHelper.COL_ROLE, u.getRole());
        return db.insert(DatabaseHelper.TABLE_UTILISATEUR, null, values);
    }

    /** Vérifie si un username est déjà pris */
    public boolean usernameExiste(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_UTILISATEUR,
                new String[]{DatabaseHelper.COL_ID},
                DatabaseHelper.COL_USERNAME + "=?",
                new String[]{username},
                null, null, null
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /** Retourne tous les utilisateurs */
    public List<Utilisateur> getAllUtilisateurs() {
        List<Utilisateur> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_UTILISATEUR,
                null, null, null, null, null,
                DatabaseHelper.COL_USERNAME + " ASC");
        while (cursor.moveToNext()) {
            list.add(cursorToUtilisateur(cursor));
        }
        cursor.close();
        return list;
    }

    /** Supprime un utilisateur par ID */
    public int deleteUtilisateur(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_UTILISATEUR,
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    private Utilisateur cursorToUtilisateur(Cursor cursor) {
        return new Utilisateur(
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USERNAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PASSWORD)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROLE))
        );
    }
}
