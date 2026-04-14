package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Client;
import com.bricolirent.domain.entity.User;
import com.bricolirent.repository.ClientRepository;
import com.bricolirent.repository.UserRepository;
import com.bricolirent.security.PasswordHashUtil;
import com.bricolirent.service.RegistrationService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger LOGGER = Logger.getLogger(RegistrationServiceImpl.class.getName());

    private UserRepository userRepository;
    private ClientRepository clientRepository;

    @PostConstruct
    public void init() {
        this.userRepository = new UserRepository();
        this.clientRepository = new ClientRepository();
        LOGGER.info("[RegistrationServiceImpl] Initialisé avec UserRepository et ClientRepository.");
    }

    @Override
    public boolean registerClient(String fullName, String email, String rawPassword) {
        if (email == null || rawPassword == null || fullName == null) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();

        // 1. Vérifier l'unicité de l'email
        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);
        if (existingUser.isPresent()) {
            LOGGER.warning("[REGISTRATION] Échec : Email déjà utilisé - " + normalizedEmail);
            return false;
        }

        try {
            // 2. Créer l'entité User de base
            User user = new User();
            user.setFullName(fullName.trim());
            user.setEmail(normalizedEmail);
            user.setPasswordHash(PasswordHashUtil.hash(rawPassword));
            user.setActive(true);

            // 4. Créer l'entité Client associée via @MapsId
            Client client = new Client();
            client.setScore(0); // Score par défaut pour un nouveau client

            // 5. Persister le User et le Client en une seule transaction
            // Cela empêche la création d'un "User fantôme" sans "Client" en cas d'erreur
            // et évite l'erreur classique "Detached Entity" car la session reste ouverte !
            clientRepository.saveUserAndClient(user, client);

            LOGGER.info("[REGISTRATION] Succès : Nouveau client enregistré - " + normalizedEmail);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[REGISTRATION] Erreur système lors de l'inscription", e);
            return false;
        }
    }
}
