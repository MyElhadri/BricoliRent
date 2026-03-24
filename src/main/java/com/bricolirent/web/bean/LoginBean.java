package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.User;
import com.bricolirent.service.AuthService;
import com.bricolirent.service.impl.AuthServiceImpl;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Optional;

/**
 * JSF managed bean for login and session management.
 */
@Named("loginBean")
@SessionScoped
public class LoginBean implements Serializable {

    private String username;
    private String password;
    private User loggedUser;

    private final AuthService authService = new AuthServiceImpl();

    /**
     * Authenticate the user and redirect based on role.
     */
    public String login() {
        Optional<User> user = authService.authenticate(username, password);
        if (user.isPresent()) {
            loggedUser = user.get();
            return "/app/dashboard.xhtml?faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Identifiants invalides", "Nom d'utilisateur ou mot de passe incorrect."));
            return null;
        }
    }

    /**
     * Logout and invalidate session.
     */
    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/login.xhtml?faces-redirect=true";
    }

    /**
     * Check if a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return loggedUser != null;
    }

    // ==================== Getters & Setters ====================

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User getLoggedUser() {
        return loggedUser;
    }

    public void setLoggedUser(User loggedUser) {
        this.loggedUser = loggedUser;
    }
}
