package com.bricolirent.web.bean;

import com.bricolirent.service.AdminUserService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named("adminUserBean")
@ViewScoped
public class AdminUserBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AdminUserService adminUserService;

    private List<AdminUserService.UserSummary> users;

    @PostConstruct
    public void init() {
        refreshUsers();
    }

    public void toggleUserActive(Long userId) {
        try {
            adminUserService.toggleUserActive(userId);
            refreshUsers();
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Le statut de l'utilisateur a ete mis a jour.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Impossible de changer le statut de l'utilisateur.");
        }
    }

    public List<AdminUserService.UserSummary> getUsers() {
        return users;
    }

    private void refreshUsers() {
        users = adminUserService.getAllUserSummaries();
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
