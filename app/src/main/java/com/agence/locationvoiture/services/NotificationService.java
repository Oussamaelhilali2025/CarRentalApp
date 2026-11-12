package com.agence.locationvoiture.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.agence.locationvoiture.R;
import com.agence.locationvoiture.activities.ReservationDetailActivity;
import com.agence.locationvoiture.activities.PaiementListActivity;

/**
 * Service centralisé pour l'envoi des notifications locales.
 * Gère les canaux de notification et l'affichage des messages.
 */
public class NotificationService {

    // Canaux de notification
    public static final String CHANNEL_RESERVATION  = "channel_reservation";
    public static final String CHANNEL_PAIEMENT     = "channel_paiement";
    public static final String CHANNEL_RAPPEL       = "channel_rappel";

    // IDs de notification (uniques)
    private static int notifId = 1000;

    private final Context context;
    private final NotificationManagerCompat notifManager;

    public NotificationService(Context context) {
        this.context     = context.getApplicationContext();
        this.notifManager = NotificationManagerCompat.from(this.context);
        createChannels();
    }

    /** Crée les canaux de notification requis pour Android 8+ */
    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);

            // Canal Réservations
            NotificationChannel chReservation = new NotificationChannel(
                    CHANNEL_RESERVATION,
                    "Réservations",
                    NotificationManager.IMPORTANCE_HIGH);
            chReservation.setDescription("Notifications de création et modification de réservations");

            // Canal Paiements
            NotificationChannel chPaiement = new NotificationChannel(
                    CHANNEL_PAIEMENT,
                    "Paiements",
                    NotificationManager.IMPORTANCE_DEFAULT);
            chPaiement.setDescription("Confirmations de paiement");

            // Canal Rappels
            NotificationChannel chRappel = new NotificationChannel(
                    CHANNEL_RAPPEL,
                    "Rappels",
                    NotificationManager.IMPORTANCE_DEFAULT);
            chRappel.setDescription("Rappels de réservation");

            nm.createNotificationChannel(chReservation);
            nm.createNotificationChannel(chPaiement);
            nm.createNotificationChannel(chRappel);
        }
    }

    /**
     * Notification de confirmation de réservation créée.
     * @param reservationId ID de la réservation créée
     * @param clientNom     Nom du client
     * @param voitureNom    Nom de la voiture
     * @param dateDebut     Date de début de la réservation
     * @param dateFin       Date de fin de la réservation
     */
    public void notifierReservationCree(int reservationId, String clientNom,
                                        String voitureNom, String dateDebut, String dateFin) {
        Intent intent = new Intent(context, ReservationDetailActivity.class);
        intent.putExtra("reservation_id", reservationId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pi = PendingIntent.getActivity(
                context, reservationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_RESERVATION)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("✅ Réservation confirmée")
                .setContentText(clientNom + " — " + voitureNom)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Réservation #" + reservationId + " créée avec succès.\n" +
                                "Client : " + clientNom + "\n" +
                                "Voiture : " + voitureNom + "\n" +
                                "Du " + dateDebut + " au " + dateFin))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            notifManager.notify(notifId++, builder.build());
        } catch (SecurityException e) {
            // Permission non accordée — notification silencieusement ignorée
        }
    }

    /**
     * Notification de confirmation d'annulation de réservation.
     */
    public void notifierReservationAnnulee(int reservationId, String voitureNom) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_RESERVATION)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("❌ Réservation annulée")
                .setContentText("Réservation #" + reservationId + " — " + voitureNom + " est maintenant disponible")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        try {
            notifManager.notify(notifId++, builder.build());
        } catch (SecurityException e) {
            // Ignoré
        }
    }

    /**
     * Notification de confirmation de paiement enregistré.
     */
    public void notifierPaiementEnregistre(int reservationId, double montant, double reste) {
        Intent intent = new Intent(context, PaiementListActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                context, reservationId + 5000, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String message = String.format("Paiement de %.2f MAD enregistré pour la réservation #%d.", montant, reservationId);
        if (reste <= 0) {
            message += " Réservation entièrement payée !";
        } else {
            message += String.format(" Reste : %.2f MAD.", reste);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_PAIEMENT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("💰 Paiement confirmé")
                .setContentText(String.format("%.2f MAD reçu — Réservation #%d", montant, reservationId))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            notifManager.notify(notifId++, builder.build());
        } catch (SecurityException e) {
            // Ignoré
        }
    }

    /**
     * Notification de rappel — réservation qui commence bientôt.
     */
    public void notifierRappelReservation(int reservationId, String clientNom,
                                           String voitureNom, String dateDebut) {
        Intent intent = new Intent(context, ReservationDetailActivity.class);
        intent.putExtra("reservation_id", reservationId);
        PendingIntent pi = PendingIntent.getActivity(
                context, reservationId + 9000, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_RAPPEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("⏰ Rappel de réservation")
                .setContentText(clientNom + " — " + voitureNom + " commence le " + dateDebut)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            notifManager.notify(notifId++, builder.build());
        } catch (SecurityException e) {
            // Ignoré
        }
    }

    /**
     * Notification de paiement en ligne réussi.
     */
    public void notifierPaiementEnLigneReussi(int reservationId, double montant, String methode) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_PAIEMENT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("💳 Paiement en ligne confirmé")
                .setContentText(String.format("%.2f MAD payé par %s — Réservation #%d", montant, methode, reservationId))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            notifManager.notify(notifId++, builder.build());
        } catch (SecurityException e) {
            // Ignoré
        }
    }
}
