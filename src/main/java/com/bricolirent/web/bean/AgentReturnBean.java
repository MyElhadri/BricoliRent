package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.ReturnRecord;
import com.bricolirent.service.ReturnService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("agentReturnBean")
@ViewScoped
public class AgentReturnBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AgentReturnBean.class.getName());

    @Inject
    private ReturnService returnService;

    @Inject
    private LoginBean loginBean;

    private List<Reservation> reservationsToReturn = Collections.emptyList();
    private List<ReturnRecord> returnHistory = Collections.emptyList();

    @PostConstruct
    public void init() {
        refreshData();
    }

    public void registerReturn(Long reservationId) {
        try {
            ReturnService.ReturnProcessResult result = returnService.enregistrerRetour(reservationId, getAgentId());
            addMessage(FacesMessage.SEVERITY_INFO, "Retour enregistre", result.message());
            refreshData();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement d'un retour", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
    }

    public List<Reservation> getReservationsToReturn() {
        return reservationsToReturn;
    }

    public List<ReturnRecord> getReturnHistory() {
        return returnHistory;
    }

    public String displayStatus(Reservation reservation) {
        if (reservation == null || reservation.getStatus() == null) {
            return "Inconnu";
        }
        return switch (reservation.getStatus()) {
            case CHECKED_OUT -> "Check-out effectue";
            case RETURNED -> "Retour enregistre";
            case APPROVED -> "Approuvee";
            case PENDING -> "En attente";
            case REJECTED -> "Rejetee";
        };
    }

    public String statusTone(Reservation reservation) {
        if (reservation == null || reservation.getStatus() == null) {
            return "neutral";
        }
        return switch (reservation.getStatus()) {
            case RETURNED -> "success";
            case CHECKED_OUT -> "warning";
            case APPROVED, PENDING -> "info";
            case REJECTED -> "danger";
        };
    }

    public String penaltyTone(ReturnRecord record) {
        return record != null && record.getLateDays() != null && record.getLateDays() > 0 ? "danger" : "success";
    }

    private void refreshData() {
        try {
            reservationsToReturn = returnService.getReservationsToReturn();
            Long agentId = getAgentId();
            returnHistory = agentId == null
                    ? Collections.emptyList()
                    : returnService.getReturnHistoryByAgent(agentId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des donnees de retour", e);
            reservationsToReturn = Collections.emptyList();
            returnHistory = Collections.emptyList();
        }
    }

    private Long getAgentId() {
        return loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
