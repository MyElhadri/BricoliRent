package com.bricolirent.service;

public interface PasswordResetService {
    
    /**
     * Initie le processus de réinitialisation.
     * Génère un token et envoie un email si l'email existe, mais
     * retourne tout le temps void ou un succès générique pour sécurité.
     */
    void requestPasswordReset(String email);

    /**
     * Valide le token et applique le nouveau mot de passe.
     * @return true si succès, false si le token est invalide, expiré ou déjà utilisé.
     */
    boolean resetPassword(String token, String newRawPassword);
}
