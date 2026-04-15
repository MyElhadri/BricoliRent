package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.service.ReservationService;
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

@Named("agentReservationBean")
@ViewScoped
public class AgentReservationBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AgentReservationBean.class.getName());

    @Inject
    private ReservationService reservationService;

    @Inject
    private LoginBean loginBean;

    private List<Reservation> pendingReservations = Collections.emptyList();
    private List<Reservation> handledReservations = Collections.emptyList();

    @PostConstruct
    public void init() {
        refreshData();
    }

    public void approve(Long reservationId) {
        try {
            reservationService.approuverDemande(reservationId, getAgentId());
            addMessage(FacesMessage.SEVERITY_INFO, "Demande approuvee", "La demande a ete approuvee avec succes.");
            refreshData();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'approbation d'une demande", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
    }

    public void reject(Long reservationId) {
        try {
            reservationService.rejeterDemande(reservationId, getAgentId(), null);
            addMessage(FacesMessage.SEVERITY_INFO, "Demande rejetee", "La demande a ete rejetee avec succes.");
            refreshData();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du rejet d'une demande", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
    }

    public List<Reservation> getPendingReservations() {
        return pendingReservations;
    }

    public List<Reservation> getHandledReservations() {
        return handledReservations;
    }

    public String displayStatus(Reservation reservation) {
        if (reservation == null || reservation.getStatus() == null) {
            return "Inconnu";
        }
        return switch (reservation.getStatus()) {
            case APPROVED -> "Approuvee";
            case REJECTED -> "Rejetee";
            case PENDING -> "En attente";
            case CHECKED_OUT -> "Check-out effectue";
            case RETURNED -> "Retournee";
        };
    }

    public String statusTone(Reservation reservation) {
        if (reservation == null || reservation.getStatus() == null) {
            return "neutral";
        }
        return switch (reservation.getStatus()) {
            case APPROVED -> "success";
            case REJECTED -> "danger";
            case PENDING -> "warning";
            case CHECKED_OUT, RETURNED -> "info";
        };
    }

    public String decisionReason(Reservation reservation) {
        if (reservation == null || reservation.getApprovalReason() == null || reservation.getApprovalReason().isBlank()) {
            return "Aucun motif detaille disponible.";
        }
        return reservation.getApprovalReason();
    }

    private void refreshData() {
        try {
            pendingReservations = reservationService.getPendingReservations();
            Long agentId = getAgentId();
            handledReservations = agentId == null
                    ? Collections.emptyList()
                    : reservationService.getHandledReservationsByAgent(agentId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des demandes agent", e);
            pendingReservations = Collections.emptyList();
            handledReservations = Collections.emptyList();
        }
    }

    private Long getAgentId() {
        return loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
