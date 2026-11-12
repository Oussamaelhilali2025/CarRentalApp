package com.agence.locationvoiture.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.agence.locationvoiture.database.DatabaseHelper;
import com.agence.locationvoiture.models.Voiture;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour les opérations CRUD sur les voitures.
 */
public class VoitureDAO {

    private final DatabaseHelper dbHelper;

    public VoitureDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    /** Ajoute une voiture. Retourne l'ID généré, ou -1 en cas d'erreur. */
    public long addVoiture(Voiture v) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = buildValues(v);
        return db.insert(DatabaseHelper.TABLE_VOITURE, null, values);
    }

    /** Met à jour une voiture existante. Retourne le nb de lignes modifiées. */
    public int updateVoiture(Voiture v) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = buildValues(v);
        return db.update(DatabaseHelper.TABLE_VOITURE, values,
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(v.getId())});
    }

    /** Supprime une voiture par son ID. */
    public int deleteVoiture(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_VOITURE,
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    /** Retourne toutes les voitures, triées par marque. */
    public List<Voiture> getAllVoitures() {
        List<Voiture> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VOITURE,
                null, null, null, null, null,
                DatabaseHelper.COL_MARQUE + " ASC");
        while (cursor.moveToNext()) {
            list.add(cursorToVoiture(cursor));
        }
        cursor.close();
        return list;
    }

    /** Retourne uniquement les voitures disponibles. */
    public List<Voiture> getVoituresDisponibles() {
        List<Voiture> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VOITURE,
                null,
                DatabaseHelper.COL_DISPONIBLE + "=?",
                new String[]{"1"},
                null, null,
                DatabaseHelper.COL_MARQUE + " ASC");
        while (cursor.moveToNext()) {
            list.add(cursorToVoiture(cursor));
        }
        cursor.close();
        return list;
    }

    /** Retourne une voiture par son ID, ou null si introuvable. */
    public Voiture getVoitureById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VOITURE,
                null,
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);
        Voiture v = null;
        if (cursor.moveToFirst()) {
            v = cursorToVoiture(cursor);
        }
        cursor.close();
        return v;
    }

    /** Recherche des voitures par marque ou modèle (insensible à la casse). */
    public List<Voiture> searchVoitures(String query) {
        List<Voiture> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String like = "%" + query + "%";
        Cursor cursor = db.query(DatabaseHelper.TABLE_VOITURE,
                null,
                DatabaseHelper.COL_MARQUE + " LIKE ? OR " + DatabaseHelper.COL_MODELE + " LIKE ?",
                new String[]{like, like},
                null, null,
                DatabaseHelper.COL_MARQUE + " ASC");
        while (cursor.moveToNext()) {
            list.add(cursorToVoiture(cursor));
        }
        cursor.close();
        return list;
    }

    /** Met à jour uniquement le statut de disponibilité d'une voiture. */
    public void setDisponibilite(int voitureId, boolean disponible) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_DISPONIBLE, disponible ? 1 : 0);
        db.update(DatabaseHelper.TABLE_VOITURE, values,
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(voitureId)});
    }

    /** Retourne le nombre total de voitures. */
    public int countAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_VOITURE, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /** Retourne le nombre de voitures disponibles. */
    public int countDisponibles() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_VOITURE +
                " WHERE " + DatabaseHelper.COL_DISPONIBLE + "=1", null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    private ContentValues buildValues(Voiture v) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_MARQUE,     v.getMarque());
        values.put(DatabaseHelper.COL_MODELE,     v.getModele());
        values.put(DatabaseHelper.COL_ANNEE,      v.getAnnee());
        values.put(DatabaseHelper.COL_PRIX_JOUR,  v.getPrixJour());
        values.put(DatabaseHelper.COL_DISPONIBLE, v.isDisponible() ? 1 : 0);
        values.put(DatabaseHelper.COL_IMAGE_PATH, v.getImagePath());
        return values;
    }

    private Voiture cursorToVoiture(Cursor cursor) {
        return new Voiture(
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MARQUE)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MODELE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ANNEE)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRIX_JOUR)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISPONIBLE)) == 1,
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_IMAGE_PATH))
        );
    }
}
