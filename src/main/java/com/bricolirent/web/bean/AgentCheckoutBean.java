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

@Named("agentCheckoutBean")
@ViewScoped
public class AgentCheckoutBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AgentCheckoutBean.class.getName());

    @Inject
    private ReservationService reservationService;

    @Inject
    private LoginBean loginBean;

    private List<Reservation> approvedReservations = Collections.emptyList();
    private List<Reservation> checkoutHistory = Collections.emptyList();

    @PostConstruct
    public void init() {
        refreshData();
    }

    public String checkout(Long reservationId) {
        try {
            reservationService.effectuerCheckout(reservationId, getAgentId());
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
            addMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Check-out effectue",
                    "La sortie du materiel a ete enregistree avec succes. Enregistrez maintenant la location et la caution depuis l'ecran Paiements cash."
            );
            refreshData();
            return "/app/agent/payments.xhtml?faces-redirect=true";
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du check-out", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
        return null;
    }

    public List<Reservation> getApprovedReservations() {
        return approvedReservations;
    }

    public List<Reservation> getCheckoutHistory() {
        return checkoutHistory;
    }

    public String displayStatus(Reservation reservation) {
        if (reservation == null || reservation.getStatus() == null) {
            return "Inconnu";
        }
        return switch (reservation.getStatus()) {
            case CHECKED_OUT -> "Check-out effectue";
            case RETURNED -> "Retournee";
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
            case CHECKED_OUT, RETURNED -> "success";
            case APPROVED -> "info";
            case PENDING -> "warning";
            case REJECTED -> "danger";
        };
    }

    private void refreshData() {
        try {
            approvedReservations = reservationService.getApprovedReservations();
            Long agentId = getAgentId();
            checkoutHistory = agentId == null
                    ? Collections.emptyList()
                    : reservationService.getCheckoutHistoryByAgent(agentId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des donnees de check-out", e);
            approvedReservations = Collections.emptyList();
            checkoutHistory = Collections.emptyList();
        }
    }

    private Long getAgentId() {
        return loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
