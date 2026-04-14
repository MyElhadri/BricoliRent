package com.bricolirent.web.bean;

import com.bricolirent.service.PasswordResetService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("resetPasswordBean")
@ViewScoped
public class ResetPasswordBean implements Serializable {

    private String token;
    private String password;
    private String confirmPassword;
    private boolean validToken = true;

    @Inject
    private PasswordResetService resetService;

    public void init() {
        if (token == null || token.trim().isEmpty()) {
            validToken = false;
        }
        // Pourrions-nous valider que le token existe en base à l'init ? Oui, mais l'utilisateur le verrait vite en cliquant
    }

    public String updatePassword() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Erreur", "Les mots de passe ne correspondent pas."
            ));
            return null;
        }

        boolean success = resetService.resetPassword(token, password);

        if (!success) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Échec", "Le lien de réinitialisation est invalide, expiré, ou a déjà été utilisé."
            ));
            return null;
        }

        context.getExternalContext().getFlash().setKeepMessages(true);
        context.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO, "Succès", "Votre mot de passe a été mis à jour avec succès. Vous pouvez maintenant vous connecter."
        ));

        return "/login.xhtml?faces-redirect=true";
    }

    // Getters / Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public boolean isValidToken() { return validToken; }
}
