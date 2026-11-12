package com.agence.locationvoiture.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utilitaires de manipulation de dates.
 * Format de stockage (DB) : yyyy-MM-dd (ISO 8601)
 * Format d'affichage (UI) : dd/MM/yyyy
 */
public class DateUtils {

    private static final DateTimeFormatter FMT_DB      = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Convertit une date ISO (DB) vers le format d'affichage */
    public static String toDisplay(String isoDate) {
        try {
            return LocalDate.parse(isoDate, FMT_DB).format(FMT_DISPLAY);
        } catch (Exception e) {
            return isoDate;
        }
    }

    /** Convertit une date d'affichage vers le format ISO (DB) */
    public static String toDb(String displayDate) {
        try {
            return LocalDate.parse(displayDate, FMT_DISPLAY).format(FMT_DB);
        } catch (Exception e) {
            return displayDate;
        }
    }

    /** Retourne la date du jour au format ISO */
    public static String today() {
        return LocalDate.now().format(FMT_DB);
    }

    /** Calcule le nombre de jours entre deux dates ISO */
    public static long daysBetween(String dateDebutIso, String dateFinIso) {
        try {
            LocalDate debut = LocalDate.parse(dateDebutIso, FMT_DB);
            LocalDate fin   = LocalDate.parse(dateFinIso, FMT_DB);
            return ChronoUnit.DAYS.between(debut, fin);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Vérifie si dateFin est strictement après dateDebut */
    public static boolean isValidRange(String dateDebutIso, String dateFinIso) {
        return daysBetween(dateDebutIso, dateFinIso) > 0;
    }

    /** Calcule le prix total d'une réservation */
    public static double calculerPrixTotal(String dateDebutIso, String dateFinIso, double prixJour) {
        long jours = daysBetween(dateDebutIso, dateFinIso);
        return jours * prixJour;
    }
}
