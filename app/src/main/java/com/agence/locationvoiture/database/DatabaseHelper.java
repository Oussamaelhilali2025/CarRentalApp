package com.agence.locationvoiture.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.agence.locationvoiture.utils.HashUtils;

/**
 * Gestionnaire SQLite — version 3
 * Seed corrigé : utilisateur_id résolu par sous-requête (pas de valeur hardcodée).
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "location_voiture.db";
    private static final int    DB_VERSION = 3;

    // Tables
    public static final String TABLE_UTILISATEUR = "utilisateurs";
    public static final String TABLE_VOITURE     = "voitures";
    public static final String TABLE_CLIENT      = "clients";
    public static final String TABLE_RESERVATION = "reservations";
    public static final String TABLE_PAIEMENT    = "paiements";

    // Colonnes communes
    public static final String COL_ID = "id";

    // Utilisateur
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";
    public static final String COL_ROLE     = "role";

    // Voiture
    public static final String COL_MARQUE     = "marque";
    public static final String COL_MODELE     = "modele";
    public static final String COL_ANNEE      = "annee";
    public static final String COL_PRIX_JOUR  = "prix_jour";
    public static final String COL_DISPONIBLE = "disponible";
    public static final String COL_IMAGE_PATH = "image_path";

    // Client
    public static final String COL_NOM            = "nom";
    public static final String COL_PRENOM         = "prenom";
    public static final String COL_EMAIL          = "email";
    public static final String COL_TELEPHONE      = "telephone";
    public static final String COL_UTILISATEUR_ID = "utilisateur_id";

    // Réservation
    public static final String COL_CLIENT_ID  = "client_id";
    public static final String COL_VOITURE_ID = "voiture_id";
    public static final String COL_DATE_DEBUT = "date_debut";
    public static final String COL_DATE_FIN   = "date_fin";
    public static final String COL_PRIX_TOTAL = "prix_total";
    public static final String COL_STATUT     = "statut";

    // Paiement
    public static final String COL_RESERVATION_ID = "reservation_id";
    public static final String COL_MONTANT        = "montant";
    public static final String COL_DATE_PAIEMENT  = "date_paiement";

    // Singleton
    private static DatabaseHelper instance;
    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) instance = new DatabaseHelper(ctx.getApplicationContext());
        return instance;
    }
    private DatabaseHelper(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = ON;");

        db.execSQL("CREATE TABLE " + TABLE_UTILISATEUR + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT NOT NULL UNIQUE, " +
                COL_PASSWORD + " TEXT NOT NULL, " +
                COL_ROLE + " TEXT NOT NULL DEFAULT 'client');");

        db.execSQL("CREATE TABLE " + TABLE_VOITURE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MARQUE + " TEXT NOT NULL, " +
                COL_MODELE + " TEXT NOT NULL, " +
                COL_ANNEE + " INTEGER NOT NULL, " +
                COL_PRIX_JOUR + " REAL NOT NULL, " +
                COL_DISPONIBLE + " INTEGER NOT NULL DEFAULT 1, " +
                COL_IMAGE_PATH + " TEXT);");

        db.execSQL("CREATE TABLE " + TABLE_CLIENT + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOM + " TEXT NOT NULL, " +
                COL_PRENOM + " TEXT NOT NULL, " +
                COL_EMAIL + " TEXT UNIQUE, " +
                COL_TELEPHONE + " TEXT NOT NULL, " +
                COL_UTILISATEUR_ID + " INTEGER, " +
                "FOREIGN KEY (" + COL_UTILISATEUR_ID + ") REFERENCES " +
                TABLE_UTILISATEUR + "(" + COL_ID + ") ON DELETE SET NULL);");

        db.execSQL("CREATE TABLE " + TABLE_RESERVATION + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CLIENT_ID + " INTEGER NOT NULL, " +
                COL_VOITURE_ID + " INTEGER NOT NULL, " +
                COL_DATE_DEBUT + " TEXT NOT NULL, " +
                COL_DATE_FIN + " TEXT NOT NULL, " +
                COL_PRIX_TOTAL + " REAL NOT NULL, " +
                COL_STATUT + " TEXT NOT NULL DEFAULT 'en_cours', " +
                "FOREIGN KEY (" + COL_CLIENT_ID + ") REFERENCES " + TABLE_CLIENT + "(" + COL_ID + "), " +
                "FOREIGN KEY (" + COL_VOITURE_ID + ") REFERENCES " + TABLE_VOITURE + "(" + COL_ID + "));");

        db.execSQL("CREATE TABLE " + TABLE_PAIEMENT + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RESERVATION_ID + " INTEGER NOT NULL, " +
                COL_MONTANT + " REAL NOT NULL, " +
                COL_DATE_PAIEMENT + " TEXT NOT NULL, " +
                "FOREIGN KEY (" + COL_RESERVATION_ID + ") REFERENCES " + TABLE_RESERVATION + "(" + COL_ID + "));");

        seedData(db);
    }

    private void seedData(SQLiteDatabase db) {
        String hashAdmin   = HashUtils.md5("admin123");
        String hashEmploye = HashUtils.md5("employe123");
        String hashClient  = HashUtils.md5("client123");

        // Utilisateurs
        db.execSQL("INSERT INTO " + TABLE_UTILISATEUR + " (username,password,role) VALUES ('admin','" + hashAdmin + "','admin');");
        db.execSQL("INSERT INTO " + TABLE_UTILISATEUR + " (username,password,role) VALUES ('employe1','" + hashEmploye + "','employe');");
        db.execSQL("INSERT INTO " + TABLE_UTILISATEUR + " (username,password,role) VALUES ('client1','" + hashClient + "','client');");

        // Client lié au compte client1 — sous-requête pour éviter hardcode de l'ID
        db.execSQL("INSERT INTO " + TABLE_CLIENT + " (nom,prenom,email,telephone,utilisateur_id) " +
                "VALUES ('Alaoui','Mohamed','mohamed.alaoui@email.com','0612345678'," +
                "(SELECT id FROM " + TABLE_UTILISATEUR + " WHERE username='client1'));");

        // Clients sans compte (ajoutés par admin)
        db.execSQL("INSERT INTO " + TABLE_CLIENT + " (nom,prenom,email,telephone) VALUES ('Benali','Fatima','fatima.benali@email.com','0623456789');");
        db.execSQL("INSERT INTO " + TABLE_CLIENT + " (nom,prenom,email,telephone) VALUES ('Cherkaoui','Youssef','youssef.ch@email.com','0634567890');");

        // Voitures
        db.execSQL("INSERT INTO " + TABLE_VOITURE + " (marque,modele,annee,prix_jour,disponible) VALUES ('Dacia','Logan',2022,250.0,1);");
        db.execSQL("INSERT INTO " + TABLE_VOITURE + " (marque,modele,annee,prix_jour,disponible) VALUES ('Renault','Clio',2023,350.0,1);");
        db.execSQL("INSERT INTO " + TABLE_VOITURE + " (marque,modele,annee,prix_jour,disponible) VALUES ('Hyundai','i20',2021,300.0,1);");
        db.execSQL("INSERT INTO " + TABLE_VOITURE + " (marque,modele,annee,prix_jour,disponible) VALUES ('Toyota','Yaris',2023,400.0,1);");
        db.execSQL("INSERT INTO " + TABLE_VOITURE + " (marque,modele,annee,prix_jour,disponible) VALUES ('Volkswagen','Polo',2022,380.0,0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Recréer la base proprement
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAIEMENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESERVATION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLIENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VOITURE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_UTILISATEUR);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) db.execSQL("PRAGMA foreign_keys = ON;");
    }
}
