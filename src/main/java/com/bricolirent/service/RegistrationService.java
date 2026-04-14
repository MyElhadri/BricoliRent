package com.bricolirent.service;

public interface RegistrationService {
    
    /**
     * Enregistre un nouveau client (crée User + Client).
     *
     * @param fullName Le nom complet
     * @param email L'adresse email
     * @param rawPassword Le mot de passe en clair (sera hashé)
     * @return true si l'inscription a réussi, false si l'email existe déjà ou autre échec métier.
     */
    boolean registerClient(String fullName, String email, String rawPassword);
    
}
