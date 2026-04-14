package com.bricolirent.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaire statique pour le hachage et la vérification de mots de passe.
 * Utilise SHA-256. Classe non instanciable.
 */
public final class PasswordHashUtil {

    private PasswordHashUtil() {
        // Classe utilitaire non instanciable
    }

    /**
     * Hache un mot de passe brut en SHA-256 (hex lowercase).
     *
     * @param rawPassword le mot de passe en clair
     * @return le hash hexadécimal SHA-256
     */
    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est garanti dans tout JDK Java 17
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Vérifie qu'un mot de passe brut correspond au hash stocké.
     *
     * @param rawPassword    le mot de passe saisi par l'utilisateur
     * @param hashedPassword le hash stocké en base de données
     * @return true si les hash correspondent
     */
    public static boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) return false;
        return hash(rawPassword).equals(hashedPassword);
    }
}
