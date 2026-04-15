package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Agent;
import com.bricolirent.service.AdminUserService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named("adminAgentBean")
@ViewScoped
public class AdminAgentBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AdminUserService adminUserService;

    private String fullName;
    private String email;
    private String initialPassword;
    private String employeeCode;
    private List<Agent> agents;

    @PostConstruct
    public void init() {
        refreshList();
    }

    public void createAgent() {
        try {
            adminUserService.createAgent(fullName, email, initialPassword, employeeCode);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Le compte agent a ete cree avec succes.");
            clearForm();
            refreshList();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
    }

    public void deleteAgent(Long agentId) {
        try {
            adminUserService.deleteAgent(agentId);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Le compte agent a ete supprime avec succes.");
            refreshList();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
    }

    public void toggleAgentActive(Long agentId) {
        try {
            adminUserService.toggleAgentActive(agentId);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Le statut du compte agent a ete mis a jour.");
            refreshList();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
    }

    public void resetForm() {
        clearForm();
    }

    private void refreshList() {
        agents = adminUserService.getAllAgents();
    }

    private void clearForm() {
        fullName = null;
        email = null;
        initialPassword = null;
        employeeCode = null;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

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

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }
}
