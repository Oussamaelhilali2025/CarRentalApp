package com.agence.locationvoiture.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaires de hashage pour la sécurité des mots de passe.
 */
public class HashUtils {

    /**
     * Hash un mot de passe en MD5.
     * En production, préférer BCrypt (via une bibliothèque externe).
     */
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hashText = number.toString(16);
            // Padding à 32 caractères
            while (hashText.length() < 32) {
                hashText = "0" + hashText;
            }
            return hashText;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 non disponible", e);
        }
    }

    /**
     * Vérifie si un mot de passe correspond à un hash MD5 stocké.
     */
    public static boolean verifyMd5(String plainPassword, String hashedPassword) {
        return md5(plainPassword).equals(hashedPassword);
    }
}
