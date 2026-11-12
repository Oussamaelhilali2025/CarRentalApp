package com.agence.locationvoiture.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.agence.locationvoiture.database.DatabaseHelper;
import com.agence.locationvoiture.models.Reservation;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour les opérations CRUD sur les réservations.
 * Gère aussi la mise à jour automatique de la disponibilité des voitures.
 */
public class ReservationDAO {

    private final DatabaseHelper dbHelper;

    public ReservationDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * Crée une réservation ET met la voiture comme indisponible.
     * Utilise une transaction pour garantir l'atomicité.
     * @return l'ID de la réservation créée, ou -1 en cas d'erreur.
     */
    public long addReservation(Reservation r) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Créer la réservation
            ContentValues rv = buildValues(r);
            long id = db.insert(DatabaseHelper.TABLE_RESERVATION, null, rv);
            if (id == -1) return -1;

            // 2. Marquer la voiture comme indisponible
            ContentValues vv = new ContentValues();
            vv.put(DatabaseHelper.COL_DISPONIBLE, 0);
            db.update(DatabaseHelper.TABLE_VOITURE, vv,
                    DatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(r.getVoitureId())});

            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    /** Met à jour une réservation existante. */
    public int updateReservation(Reservation r) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(DatabaseHelper.TABLE_RESERVATION, buildValues(r),
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(r.getId())});
    }

    /**
     * Annule une réservation ET remet la voiture disponible.
     * Utilise une transaction atomique.
     */
    public boolean annulerReservation(int reservationId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Récupérer l'ID de la voiture
            Cursor cursor = db.query(DatabaseHelper.TABLE_RESERVATION,
                    new String[]{DatabaseHelper.COL_VOITURE_ID},
                    DatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(reservationId)},
                    null, null, null);
            if (!cursor.moveToFirst()) { cursor.close(); return false; }
            int voitureId = cursor.getInt(0);
            cursor.close();

            // 2. Changer le statut de la réservation
            ContentValues rv = new ContentValues();
            rv.put(DatabaseHelper.COL_STATUT, Reservation.STATUT_ANNULEE);
            db.update(DatabaseHelper.TABLE_RESERVATION, rv,
                    DatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(reservationId)});

            // 3. Remettre la voiture disponible
            ContentValues vv = new ContentValues();
            vv.put(DatabaseHelper.COL_DISPONIBLE, 1);
            db.update(DatabaseHelper.TABLE_VOITURE, vv,
                    DatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(voitureId)});

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** Marque une réservation comme terminée et remet la voiture disponible. */
    public boolean terminerReservation(int reservationId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor cursor = db.query(DatabaseHelper.TABLE_RESERVATION,
                    new String[]{DatabaseHelper.COL_VOITURE_ID},
                    DatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(reservationId)},
                    null, null, null);
            if (!cursor.moveToFirst()) { cursor.close(); return false; }
            int voitureId = cursor.getInt(0);
            cursor.close();

            ContentValues rv = new ContentValues();
            rv.put(DatabaseHelper.COL_STATUT, Reservation.STATUT_TERMINEE);
            db.update(DatabaseHelper.TABLE_RESERVATION, rv,
                    DatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(reservationId)});

            ContentValues vv = new ContentValues();
            vv.put(DatabaseHelper.COL_DISPONIBLE, 1);
            db.update(DatabaseHelper.TABLE_VOITURE, vv,
                    DatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(voitureId)});

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** Retourne toutes les réservations avec noms client et voiture (JOIN). */
    public List<Reservation> getAllReservations() {
        return queryWithJoin(null, null, null);
    }

    /** Retourne les réservations d'un client donné. */
    public List<Reservation> getReservationsByClient(int clientId) {
        return queryWithJoin("r." + DatabaseHelper.COL_CLIENT_ID + "=?",
                new String[]{String.valueOf(clientId)}, null);
    }

    /** Retourne les réservations filtrées par statut. */
    public List<Reservation> getReservationsByStatut(String statut) {
        return queryWithJoin("r." + DatabaseHelper.COL_STATUT + "=?",
                new String[]{statut}, null);
    }

    /** Retourne une réservation par son ID. */
    public Reservation getReservationById(int id) {
        List<Reservation> list = queryWithJoin(
                "r." + DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)}, null);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Retourne le nombre de réservations actives (en_cours). */
    public int countEnCours() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_RESERVATION +
                " WHERE " + DatabaseHelper.COL_STATUT + "=?",
                new String[]{Reservation.STATUT_EN_COURS});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /** Vérifie si une voiture est déjà réservée sur une plage de dates. */
    public boolean voitureDejaReservee(int voitureId, String dateDebut, String dateFin, int excludeReservationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_RESERVATION +
                " WHERE " + DatabaseHelper.COL_VOITURE_ID + "=?" +
                " AND " + DatabaseHelper.COL_STATUT + "!=?" +
                " AND id!=?" +
                " AND NOT (" + DatabaseHelper.COL_DATE_FIN + "<=? OR " + DatabaseHelper.COL_DATE_DEBUT + ">=?)",
                new String[]{
                    String.valueOf(voitureId),
                    Reservation.STATUT_ANNULEE,
                    String.valueOf(excludeReservationId),
                    dateDebut,
                    dateFin
                }
        );
        boolean result = false;
        if (cursor.moveToFirst()) result = cursor.getInt(0) > 0;
        cursor.close();
        return result;
    }

    // ─── Requête JOIN réutilisable ────────────────────────────────────────────

    private List<Reservation> queryWithJoin(String where, String[] args, String orderBy) {
        List<Reservation> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT r.*, " +
                "c." + DatabaseHelper.COL_NOM + " || ' ' || c." + DatabaseHelper.COL_PRENOM + " AS client_nom, " +
                "v." + DatabaseHelper.COL_MARQUE + " || ' ' || v." + DatabaseHelper.COL_MODELE + " AS voiture_nom " +
                "FROM " + DatabaseHelper.TABLE_RESERVATION + " r " +
                "JOIN " + DatabaseHelper.TABLE_CLIENT  + " c ON r." + DatabaseHelper.COL_CLIENT_ID  + "=c." + DatabaseHelper.COL_ID + " " +
                "JOIN " + DatabaseHelper.TABLE_VOITURE + " v ON r." + DatabaseHelper.COL_VOITURE_ID + "=v." + DatabaseHelper.COL_ID;

        if (where != null) sql += " WHERE " + where;
        sql += " ORDER BY r." + DatabaseHelper.COL_DATE_DEBUT + " DESC";

        Cursor cursor = db.rawQuery(sql, args);
        while (cursor.moveToNext()) {
            Reservation r = new Reservation(
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CLIENT_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VOITURE_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE_DEBUT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE_FIN)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRIX_TOTAL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STATUT))
            );
            r.setClientNom(cursor.getString(cursor.getColumnIndexOrThrow("client_nom")));
            r.setVoitureNom(cursor.getString(cursor.getColumnIndexOrThrow("voiture_nom")));
            list.add(r);
        }
        cursor.close();
        return list;
    }

    private ContentValues buildValues(Reservation r) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_CLIENT_ID,   r.getClientId());
        values.put(DatabaseHelper.COL_VOITURE_ID,  r.getVoitureId());
        values.put(DatabaseHelper.COL_DATE_DEBUT,  r.getDateDebut());
        values.put(DatabaseHelper.COL_DATE_FIN,    r.getDateFin());
        values.put(DatabaseHelper.COL_PRIX_TOTAL,  r.getPrixTotal());
        values.put(DatabaseHelper.COL_STATUT,      r.getStatut());
        return values;
    }

    /** Compte les réservations en cours pour un client précis. */
    public int countEnCoursByClient(int clientId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_RESERVATION +
                " WHERE " + DatabaseHelper.COL_CLIENT_ID + "=? AND " +
                DatabaseHelper.COL_STATUT + "=?",
                new String[]{String.valueOf(clientId), Reservation.STATUT_EN_COURS});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

}
