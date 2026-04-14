package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Admin;
import com.bricolirent.domain.entity.Agent;
import com.bricolirent.domain.entity.Client;
import com.bricolirent.domain.entity.User;
import com.bricolirent.repository.UserRepository;
import com.bricolirent.security.PasswordHashUtil;
import com.bricolirent.service.AuthService;
import com.bricolirent.util.HibernateUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.Session;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implémentation du service d'authentification.
 *
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Valider les identifiants via {@link UserRepository}</li>
 *   <li>Comparer le hash SHA-256 du mot de passe</li>
 *   <li>Résoudre le rôle via les tables de spécialisation Hibernate</li>
 * </ul>
 *
 * <p>Ce service ne touche PAS à la session HTTP.
 * La session est gérée exclusivement par {@code LoginBean}.</p>
 *
 * <p><strong>CDI / Weld :</strong> Le repository est initialisé via {@code @PostConstruct}
 * et NON en field initializer. Raison : Weld appelle {@code super()} lors de la création
 * de son proxy client. Un field initializer comme {@code = new UserRepository()}
 * déclencherait {@code HibernateUtil.<clinit>} avant que la base soit prête,
 * causant {@code ExceptionInInitializerError} → WELD-000033.
 * Avec {@code @PostConstruct}, l'initialisation est différée à la première
 * utilisation réelle du bean.</p>
 */
@ApplicationScoped
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthServiceImpl.class.getName());

    /** Clé de session HTTP pour l'utilisateur connecté (lue aussi par AuthenticationFilter) */
    public static final String SESSION_USER_KEY = "SESSION_USER";

    /** Clé de session HTTP pour le rôle de l'utilisateur connecté */
    public static final String SESSION_ROLE_KEY = "SESSION_ROLE";

    /**
     * Initialisé dans {@link #init()} et non en field initializer.
     * Voir Javadoc de classe pour l'explication CDI/Weld.
     */
    private UserRepository userRepository;

    /**
     * Initialisation différée : appelée uniquement sur l'instance RÉELLE,
     * jamais sur le proxy Weld → Hibernate n'est initialisé qu'à la première
     * utilisation effective du bean.
     */
    @PostConstruct
    public void init() {
        this.userRepository = new UserRepository();
        LOGGER.info("[AuthServiceImpl] Initialisé — UserRepository prêt.");
    }

    // =====================================================================
    // login()
    // =====================================================================

    @Override
    public User login(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            return null;
        }

        // 1. Recherche par email (trim + lowercase pour robustesse)
        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            LOGGER.info("[AUTH] Échec : utilisateur introuvable — email=" + email);
            return null;
        }

        User user = userOpt.get();

        // 2. Compte actif ?
        if (!Boolean.TRUE.equals(user.getActive())) {
            LOGGER.info("[AUTH] Échec : compte inactif — email=" + email);
            return null;
        }

        // 3. Vérification du mot de passe (SHA-256)
        if (!PasswordHashUtil.matches(rawPassword, user.getPasswordHash())) {
            LOGGER.info("[AUTH] Échec : mot de passe incorrect — email=" + email);
            return null;
        }

        LOGGER.info("[AUTH] Succès — email=" + email + " | id=" + user.getId());
        return user;
    }

    // =====================================================================
    // resolveRole()
    // =====================================================================

    /**
     * Ouvre une session Hibernate dédiée (openSession, pas getCurrentSession)
     * pour éviter tout conflit avec le contexte transactionnel en cours.
     */
    @Override
    public String resolveRole(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            if (session.get(Admin.class, userId) != null) {
                return "ADMIN";
            }
            if (session.get(Agent.class, userId) != null) {
                return "AGENT";
            }
            if (session.get(Client.class, userId) != null) {
                return "CLIENT";
            }
            LOGGER.warning("[AUTH] Aucun rôle trouvé pour userId=" + userId);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[AUTH] Erreur résolution rôle pour userId=" + userId, e);
            return null;
        }
    }
}
