package com.bricolirent.service;

import com.bricolirent.domain.entity.User;

/**
 * Contrat du service d'authentification — logique métier pure, sans session.
 *
 * <p>La gestion de la session HTTP (stockage, invalidation) est laissée
 * au {@code LoginBean} (couche Web) via {@code FacesContext.getExternalContext()}.</p>
 */
public interface AuthService {

    /**
     * Valide les identifiants (email + mot de passe en clair).
     * Vérifie aussi que le compte est actif.
     *
     * @param email       adresse email de l'utilisateur
     * @param rawPassword mot de passe en clair (sera hashé en SHA-256 pour comparaison)
     * @return l'entité {@link User} si authentification réussie, {@code null} sinon
     */
    User login(String email, String rawPassword);

    /**
     * Résout le rôle d'un utilisateur en interrogeant les tables de spécialisation.
     * Ordre de priorité : ADMIN → AGENT → CLIENT.
     *
     * @param userId identifiant de l'utilisateur
     * @return {@code "ADMIN"}, {@code "AGENT"}, {@code "CLIENT"}, ou {@code null}
     */
    String resolveRole(Long userId);
}
