package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.service.PaymentService;
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

@Named("agentCheckoutBean")
@ViewScoped
public class AgentCheckoutBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AgentCheckoutBean.class.getName());

    @Inject
    private ReservationService reservationService;

    @Inject
    private PaymentService paymentService;

    @Inject
    private LoginBean loginBean;

    private List<Reservation> approvedReservations = Collections.emptyList();
    private List<Reservation> checkoutHistory = Collections.emptyList();
    private String selectedFilter;
    private String searchKeyword;

    @PostConstruct
    public void init() {
        refreshData();
    }

    public String checkout(Long reservationId) {
        try {
            reservationService.effectuerCheckout(reservationId, getAgentId());
            addMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Check-out effectue",
                    "La sortie du materiel a ete enregistree avec succes."
            );
            refreshData();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du check-out", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
        return null;
    }

    public void encaisserAvantCheckout(Long reservationId) {
        try {
            paymentService.encaisserAvantCheckout(reservationId, getAgentId());
            addMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Encaissement enregistre",
                    "La location et la caution ont ete encaissees avec succes. Vous pouvez maintenant effectuer le check-out."
            );
            refreshData();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'encaissement avant check-out", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
    }

    public List<Reservation> getApprovedReservations() {
        return approvedReservations;
    }

    public List<Reservation> getCheckoutHistory() {
        return checkoutHistory;
    }

    public List<Reservation> getFilteredApprovedReservations() {
        return approvedReservations;
    }

    public List<Reservation> getFilteredCheckoutHistory() {
        return checkoutHistory.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesHistoryFilter)
                .collect(Collectors.toList());
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

    public boolean canCheckout(Reservation reservation) {
        return reservation != null
                && reservation.getId() != null
                && reservationService.isCheckoutReady(reservation.getId());
    }

    public boolean requiresPayment(Reservation reservation) {
        return !canCheckout(reservation);
    }

    public String paymentStateLabel(Reservation reservation) {
        return canCheckout(reservation) ? "Paiement complet" : "Encaissement requis";
    }

    public String paymentStateTone(Reservation reservation) {
        return canCheckout(reservation) ? "success" : "warning";
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

    private boolean matchesApprovedFilter(Reservation reservation) {
        return true;
    }

    private boolean matchesHistoryFilter(Reservation reservation) {
        if (selectedFilter == null || selectedFilter.isBlank()) {
            return true;
        }
        if (reservation == null || reservation.getStatus() == null) {
            return false;
        }
        return switch (selectedFilter) {
            case "CHECKED_OUT" -> reservation.getStatus().name().equals("CHECKED_OUT");
            case "RETURNED" -> reservation.getStatus().name().equals("RETURNED");
            default -> true;
        };
    }
}
