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
import java.util.Locale;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    private String selectedFilter;
    private String searchKeyword;

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

    public List<Reservation> getFilteredPendingReservations() {
        return pendingReservations;
    }

    public List<Reservation> getFilteredHandledReservations() {
        return handledReservations.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesHandledFilter)
                .collect(Collectors.toList());
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

    public String getSelectedFilter() {
        return selectedFilter;
    }

    public void setSelectedFilter(String selectedFilter) {
        this.selectedFilter = selectedFilter;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public void applyFilter() {
        // Filtrage calcule a la demande.
    }

    public boolean filterActive(String filterKey) {
        if (filterKey == null || filterKey.isBlank()) {
            return selectedFilter == null || selectedFilter.isBlank();
        }
        return filterKey.equalsIgnoreCase(selectedFilter);
    }

    public String imageName(Reservation reservation) {
        if (reservation == null || reservation.getTool() == null || reservation.getTool().getImagePath() == null) {
            return "default-tool.jpg";
        }
        String imagePath = reservation.getTool().getImagePath().trim();
        return imagePath.isEmpty() ? "default-tool.jpg" : imagePath;
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

    private boolean matchesSearch(Reservation reservation) {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return true;
        }
        if (reservation == null) {
            return false;
        }
        String keyword = searchKeyword.toLowerCase(Locale.ROOT).trim();
        String toolName = reservation.getTool() != null && reservation.getTool().getName() != null
                ? reservation.getTool().getName().toLowerCase(Locale.ROOT) : "";
        String clientName = reservation.getClient() != null
                && reservation.getClient().getUsers() != null
                && reservation.getClient().getUsers().getFullName() != null
                ? reservation.getClient().getUsers().getFullName().toLowerCase(Locale.ROOT) : "";
        return toolName.contains(keyword) || clientName.contains(keyword);
    }

    private boolean matchesPendingFilter(Reservation reservation) {
        return true;
    }

    private boolean matchesHandledFilter(Reservation reservation) {
        if (selectedFilter == null || selectedFilter.isBlank()) {
            return true;
        }
        if (reservation == null || reservation.getStatus() == null) {
            return false;
        }
        return switch (selectedFilter) {
            case "APPROVED" -> reservation.getStatus().name().equals("APPROVED");
            case "REJECTED" -> reservation.getStatus().name().equals("REJECTED");
            case "PENDING" -> false;
            default -> true;
        };
    }
}
