package com.bricolirent.web.bean;

import com.bricolirent.service.PasswordResetService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("forgotPasswordBean")
@RequestScoped
public class ForgotPasswordBean implements Serializable {

    private String email;

    @Inject
    private PasswordResetService resetService;

    public String submitRequest() {
        // Envoi de la demande, qui échouera silencieusement si email non trouvé.
        resetService.requestPasswordReset(email);

        // Afficher toujours le même message de succès pour ne pas lister les e-mails valides
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getFlash().setKeepMessages(true);
        context.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                "Demande envoyée",
                "Si un compte est associé à cette adresse e-mail, vous recevrez un lien de réinitialisation d'ici quelques minutes."
        ));

        // On peut soit rediriger sur le login, soit rester sur la même page
        return "/forgot-password.xhtml?faces-redirect=true";
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
