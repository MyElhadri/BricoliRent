package com.bricolirent.web.bean;

import com.bricolirent.service.RegistrationService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.logging.Logger;

@Named("registerBean")
@RequestScoped
public class RegisterBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(RegisterBean.class.getName());

    // Champs du formulaire
    private String fullName;
    private String email;
    private String password;
    private String confirmPassword;

    @Inject
    private RegistrationService registrationService;

    public String register() {
        FacesContext context = FacesContext.getCurrentInstance();

        // 1. Validation de base : mots de passe identiques
        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Erreur de mot de passe",
                    "Les mots de passe ne correspondent pas."
            ));
            return null; // Reste sur la page
        }

        // 2. Appel au service d'inscription
        boolean success = registrationService.registerClient(fullName, email, password);

        if (!success) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Échec de l'inscription",
                    "Cette adresse e-mail est peut-être déjà utilisée ou une erreur est survenue."
            ));
            return null; // Reste sur la page
        }

        // 3. Succès : ajout d'un message flash et redirection
        context.getExternalContext().getFlash().setKeepMessages(true);
        context.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                "Inscription réussie !",
                "Votre compte a été créé avec succès. Vous pouvez maintenant vous connecter."
        ));

        // Nettoyage sécurisé
        this.password = null;
        this.confirmPassword = null;

        return "/login.xhtml?faces-redirect=true";
    }

    // Getters et Setters

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
