package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.PasswordResetToken;
import com.bricolirent.domain.entity.User;
import com.bricolirent.repository.PasswordResetTokenRepository;
import com.bricolirent.repository.UserRepository;
import com.bricolirent.security.PasswordHashUtil;
import com.bricolirent.service.MailService;
import com.bricolirent.service.PasswordResetService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger LOGGER = Logger.getLogger(PasswordResetServiceImpl.class.getName());

    private UserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;

    @Inject
    private MailService mailService;

    @PostConstruct
    public void init() {
        this.userRepository = new UserRepository();
        this.tokenRepository = new PasswordResetTokenRepository();
    }

    @Override
    public void requestPasswordReset(String email) {
        if (email == null) return;
        
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        
        // Sécurité : Ne jamais révéler si l'email existe, on s'arrête silencieusement
        if (userOpt.isEmpty()) {
            LOGGER.info("[RESET] Demande de reset ignorée (email inconnu) : " + normalizedEmail);
            return;
        }

        User user = userOpt.get();

        // 1. Générer le token
        String token = UUID.randomUUID().toString();
        
        // Expiration = +24 heures
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

        // 2. Vérifier si un token existe déjà et le mettre à jour ou le créer
        Optional<PasswordResetToken> existingTokenOpt = tokenRepository.findByUser(user);
        if (existingTokenOpt.isPresent()) {
            PasswordResetToken existingToken = existingTokenOpt.get();
            existingToken.setToken(token);
            existingToken.setExpiryDate(expiryDate);
            existingToken.setUsed(false);
            tokenRepository.update(existingToken);
        } else {
            PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
            tokenRepository.save(resetToken);
        }

        // 3. Envoyer l'email
        // On construit l'URL de réinitialisation. Adapter localhost/bricolirent_war/ au besoin
        String resetUrl = "http://localhost:8080/bricolirent/reset-password.xhtml?token=" + token;
        
        String content = "Bonjour " + user.getFullName() + ",\n\n"
                + "Vous avez demandé la réinitialisation de votre mot de passe sur BricoliRent.\n"
                + "Cliquez sur le lien suivant (valide 24h) pour changer votre mot de passe :\n\n"
                + resetUrl + "\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.\n\n"
                + "L'équipe BricoliRent";

        mailService.sendTextMail(normalizedEmail, "BricoliRent : Réinitialisation de votre mot de passe", content);
    }

    @Override
    public boolean resetPassword(String token, String newRawPassword) {
        if (token == null || newRawPassword == null || newRawPassword.trim().isEmpty()) {
            return false;
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            LOGGER.warning("[RESET] Token introuvable : " + token);
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isUsed() || resetToken.isExpired()) {
            LOGGER.warning("[RESET] Token expiré ou déjà utilisé : " + token);
            return false;
        }

        try {
            // Modification du mot de passe
            User user = resetToken.getUser();
            user.setPasswordHash(PasswordHashUtil.hash(newRawPassword));
            userRepository.update(user); // Met à jour le user dans la base

            // Invalidation du token pour usage unique
            resetToken.setUsed(true);
            tokenRepository.update(resetToken);
            
            LOGGER.info("[RESET] Mot de passe réinitialisé pour l'utilisateur ID: " + user.getId());
            return true;
        } catch (Exception e) {
            LOGGER.severe("[RESET] Erreur lors de la mise à jour du mot de passe: " + e.getMessage());
            return false;
        }
    }
}
