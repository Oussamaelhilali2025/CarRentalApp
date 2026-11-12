package com.agence.locationvoiture.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.agence.locationvoiture.database.DatabaseHelper;
import com.agence.locationvoiture.models.Paiement;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour les opérations sur les paiements.
 *
 * Gestion des rôles (au niveau DAO) :
 *  - getAllPaiements          : admin + employé (toutes les transactions)
 *  - getPaiementsByUserId     : client (seulement ses propres paiements)
 *  - getPaiementsByReservation: admin + employé (détail d'une réservation)
 *  - deletePaiement           : admin uniquement (enforced dans l'Activity)
 */
public class PaiementDAO {

    private final DatabaseHelper dbHelper;

    public PaiementDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    /** Enregistre un paiement. Retourne l'ID généré. */
    public long addPaiement(Paiement p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_RESERVATION_ID, p.getReservationId());
        values.put(DatabaseHelper.COL_MONTANT,         p.getMontant());
        values.put(DatabaseHelper.COL_DATE_PAIEMENT,   p.getDatePaiement());
        return db.insert(DatabaseHelper.TABLE_PAIEMENT, null, values);
    }

    /** Supprime un paiement (admin uniquement). */
    public int deletePaiement(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_PAIEMENT,
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    /** Paiements d'une réservation précise (pour le détail de réservation). */
    public List<Paiement> getPaiementsByReservation(int reservationId) {
        List<Paiement> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_PAIEMENT, null,
                DatabaseHelper.COL_RESERVATION_ID + "=?",
                new String[]{String.valueOf(reservationId)},
                null, null,
                DatabaseHelper.COL_DATE_PAIEMENT + " DESC");
        while (cursor.moveToNext()) list.add(cursorToPaiement(cursor));
        cursor.close();
        return list;
    }

    /**
     * Tous les paiements — pour admin et employé.
     * Inclut les infos de réservation et client via JOIN.
     */
    public List<Paiement> getAllPaiements() {
        List<Paiement> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql =
            "SELECT p.*, " +
            "  (c." + DatabaseHelper.COL_PRENOM + " || ' ' || c." + DatabaseHelper.COL_NOM + ") AS client_nom, " +
            "  (v." + DatabaseHelper.COL_MARQUE + " || ' ' || v." + DatabaseHelper.COL_MODELE + ") AS voiture_nom " +
            "FROM " + DatabaseHelper.TABLE_PAIEMENT + " p " +
            "JOIN " + DatabaseHelper.TABLE_RESERVATION + " r ON p." + DatabaseHelper.COL_RESERVATION_ID + " = r." + DatabaseHelper.COL_ID + " " +
            "JOIN " + DatabaseHelper.TABLE_CLIENT + " c ON r." + DatabaseHelper.COL_CLIENT_ID + " = c." + DatabaseHelper.COL_ID + " " +
            "JOIN " + DatabaseHelper.TABLE_VOITURE + " v ON r." + DatabaseHelper.COL_VOITURE_ID + " = v." + DatabaseHelper.COL_ID + " " +
            "ORDER BY p." + DatabaseHelper.COL_DATE_PAIEMENT + " DESC";
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Paiement p = cursorToPaiement(cursor);
            int colClient  = cursor.getColumnIndex("client_nom");
            int colVoiture = cursor.getColumnIndex("voiture_nom");
            if (colClient  >= 0) p.setClientNom(cursor.getString(colClient));
            if (colVoiture >= 0) p.setVoitureNom(cursor.getString(colVoiture));
            p.setReservationInfo("Réservation #" + p.getReservationId());
            list.add(p);
        }
        cursor.close();
        return list;
    }

    /**
     * Paiements d'un client connecté — pour l'espace client.
     * Filtre via utilisateur_id → client_id → reservation_id → paiement.
     * @param utilisateurId L'ID utilisateur de la session (sessionManager.getUserId())
     */
    public List<Paiement> getPaiementsByUserId(int utilisateurId) {
        List<Paiement> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql =
            "SELECT p.*, " +
            "  (v." + DatabaseHelper.COL_MARQUE + " || ' ' || v." + DatabaseHelper.COL_MODELE + ") AS voiture_nom, " +
            "  r." + DatabaseHelper.COL_DATE_DEBUT + " AS date_debut, " +
            "  r." + DatabaseHelper.COL_DATE_FIN   + " AS date_fin " +
            "FROM " + DatabaseHelper.TABLE_PAIEMENT + " p " +
            "JOIN " + DatabaseHelper.TABLE_RESERVATION + " r ON p." + DatabaseHelper.COL_RESERVATION_ID + " = r." + DatabaseHelper.COL_ID + " " +
            "JOIN " + DatabaseHelper.TABLE_CLIENT  + " c ON r." + DatabaseHelper.COL_CLIENT_ID  + " = c." + DatabaseHelper.COL_ID + " " +
            "JOIN " + DatabaseHelper.TABLE_VOITURE + " v ON r." + DatabaseHelper.COL_VOITURE_ID + " = v." + DatabaseHelper.COL_ID + " " +
            "WHERE c." + DatabaseHelper.COL_UTILISATEUR_ID + " = ? " +
            "ORDER BY p." + DatabaseHelper.COL_DATE_PAIEMENT + " DESC";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(utilisateurId)});
        while (cursor.moveToNext()) {
            Paiement p = cursorToPaiement(cursor);
            int colVoiture   = cursor.getColumnIndex("voiture_nom");
            int colDateDebut = cursor.getColumnIndex("date_debut");
            int colDateFin   = cursor.getColumnIndex("date_fin");
            if (colVoiture   >= 0) p.setVoitureNom(cursor.getString(colVoiture));
            if (colDateDebut >= 0 && colDateFin >= 0) {
                p.setReservationInfo("Réservation #" + p.getReservationId() +
                        " (" + cursor.getString(colDateDebut) + " → " + cursor.getString(colDateFin) + ")");
            }
            list.add(p);
        }
        cursor.close();
        return list;
    }

    /** Total payé pour une réservation précise. */
    public double getTotalPaye(int reservationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_MONTANT + ") FROM " + DatabaseHelper.TABLE_PAIEMENT +
                " WHERE " + DatabaseHelper.COL_RESERVATION_ID + "=?",
                new String[]{String.valueOf(reservationId)});
        double total = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    private Paiement cursorToPaiement(Cursor cursor) {
        return new Paiement(
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_RESERVATION_ID)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MONTANT)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE_PAIEMENT))
        );
    }
}
