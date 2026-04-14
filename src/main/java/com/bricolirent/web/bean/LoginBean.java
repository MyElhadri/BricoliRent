package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.User;
import com.bricolirent.service.AuthService;
import com.bricolirent.service.impl.AuthServiceImpl;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Bean JSF de gestion de l'authentification et de la session utilisateur.
 *
 * <p>Portée {@code @SessionScoped} : une instance par session HTTP.
 * Doit implémenter {@link Serializable} (requis par CDI pour les beans session).</p>
 *
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Formulaire login : champs {@code email} / {@code password}</li>
 *   <li>Appel à {@link AuthService} pour valider les identifiants</li>
 *   <li>Stockage de l'utilisateur et du rôle dans les champs du bean
 *       ET dans la session HTTP (pour {@code AuthenticationFilter})</li>
 *   <li>Redirection vers la page d'accueil correspondant au rôle</li>
 *   <li>Déconnexion via {@code ExternalContext.invalidateSession()}</li>
 * </ul>
 *
 * <p>La gestion de session passe uniquement par {@code FacesContext.getExternalContext()}
 * — aucune injection de {@code HttpServletRequest} ici.</p>
 */
@Named("loginBean")
@SessionScoped
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(LoginBean.class.getName());

    // =====================================================================
    // Champs du formulaire de connexion
    // =====================================================================

    private String email;
    private String password;

    // =====================================================================
    // État de session (accessible depuis les pages XHTML via EL)
    // =====================================================================

    /** Utilisateur actuellement connecté (null si personne) */
    private User currentUser;

    /** Rôle de l'utilisateur connecté : "ADMIN", "AGENT" ou "CLIENT" */
    private String currentRole;

    // =====================================================================
    // Injection CDI
    // =====================================================================

    /** Service d'authentification — logique métier pure */
    @Inject
    private AuthService authService;

    // =====================================================================
    // Actions JSF
    // =====================================================================

    /**
     * Action de connexion déclenchée par le bouton du formulaire {@code login.xhtml}.
     *
     * <ol>
     *   <li>Valide les identifiants via {@link AuthService#login}</li>
     *   <li>Résout le rôle via {@link AuthService#resolveRole}</li>
     *   <li>Stocke l'utilisateur et le rôle dans le bean ET dans la session HTTP</li>
     *   <li>Redirige vers la page d'accueil du rôle</li>
     * </ol>
     *
     * @return outcome JSF (navigation avec redirect), ou {@code null} si échec
     */
    public String login() {
        FacesContext context = FacesContext.getCurrentInstance();

        // 1. Validation des identifiants
        User user = authService.login(email, password);
        if (user == null) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Identifiants invalides",
                    "Email ou mot de passe incorrect, ou compte désactivé."
            ));
            password = null; // toujours effacer le mot de passe
            return null;     // rester sur login.xhtml
        }

        // 2. Résolution du rôle
        String role = authService.resolveRole(user.getId());
        if (role == null) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Accès refusé",
                    "Aucun rôle assigné à ce compte. Contactez l'administrateur."
            ));
            password = null;
            return null;
        }

        // 3. Stocker dans le bean de session
        this.currentUser = user;
        this.currentRole  = role;

        // 4. Stocker aussi dans la session HTTP (pour AuthenticationFilter)
        ExternalContext ec = context.getExternalContext();
        HttpSession session = (HttpSession) ec.getSession(true);
        session.setAttribute(AuthServiceImpl.SESSION_USER_KEY, user);
        session.setAttribute(AuthServiceImpl.SESSION_ROLE_KEY, role);

        // 5. Effacer mot de passe de la mémoire
        password = null;

        LOGGER.info("[LoginBean] Connexion réussie — " + user.getEmail() + " | rôle=" + role);

        // 6. Redirection selon le rôle
        return switch (role) {
            case "ADMIN"  -> "/app/admin/home.xhtml?faces-redirect=true";
            case "AGENT"  -> "/app/agent/home.xhtml?faces-redirect=true";
            case "CLIENT" -> "/app/client/home.xhtml?faces-redirect=true";
            default       -> null;
        };
    }

    /**
     * Action de déconnexion.
     * Invalide la session HTTP puis réinitialise l'état du bean.
     *
     * @return navigation vers {@code index.xhtml} avec redirect
     */
    public String logout() {
        LOGGER.info("[LoginBean] Déconnexion de : " +
                (currentUser != null ? currentUser.getEmail() : "inconnu"));

        // Invalider la session HTTP (supprime aussi les attributs SESSION_USER, SESSION_ROLE)
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();

        // Réinitialiser l'état local
        currentUser = null;
        currentRole  = null;
        email        = null;
        password     = null;

        return "/index.xhtml?faces-redirect=true";
    }

    // =====================================================================
    // Méthodes utilitaires pour les pages XHTML
    // =====================================================================

    /**
     * @return {@code true} si un utilisateur est actuellement connecté
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // =====================================================================
    // Getters / Setters
    // =====================================================================

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public User getCurrentUser() { return currentUser; }

    public String getCurrentRole() { return currentRole; }
}
