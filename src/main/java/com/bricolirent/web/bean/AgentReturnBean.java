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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    private String selectedFilter;
    private String searchKeyword;

    @PostConstruct
    public void init() {
        refreshData();
    }

    public String registerReturn(Long reservationId) {
        try {
            ReturnService.ReturnProcessResult result = returnService.enregistrerRetour(reservationId, getAgentId());
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
            addMessage(FacesMessage.SEVERITY_INFO, "Retour enregistre", buildReturnFollowUpMessage(result));
            refreshData();
            return "/app/agent/payments.xhtml?faces-redirect=true";
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement d'un retour", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
        return null;
    }

    public List<Reservation> getReservationsToReturn() {
        return reservationsToReturn;
    }

    public List<ReturnRecord> getReturnHistory() {
        return returnHistory;
    }

    public List<Reservation> getFilteredReservationsToReturn() {
        return reservationsToReturn;
    }

    public List<ReturnRecord> getFilteredReturnHistory() {
        return returnHistory.stream()
                .filter(this::matchesRecordSearch)
                .filter(this::matchesHistoryFilter)
                .collect(Collectors.toList());
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

    public String imageName(ReturnRecord record) {
        return record == null ? "default-tool.jpg" : imageName(record.getReservation());
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

    private String buildReturnFollowUpMessage(ReturnService.ReturnProcessResult result) {
        if (result == null) {
            return "Le retour a ete enregistre. Verifiez les mouvements de paiement associes.";
        }
        if (result.latePenalty() != null && result.latePenalty().compareTo(BigDecimal.ZERO) > 0) {
            return result.message() + " Enregistrez maintenant la penalite depuis l'ecran Paiements cash.";
        }
        return result.message() + " Si la caution a deja ete encaissee, vous pouvez maintenant tracer son remboursement depuis l'ecran Paiements cash.";
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

    private boolean matchesRecordSearch(ReturnRecord record) {
        return record != null && matchesSearch(record.getReservation());
    }

    private boolean matchesToReturnFilter(Reservation reservation) {
        return true;
    }

    private boolean matchesHistoryFilter(ReturnRecord record) {
        if (selectedFilter == null || selectedFilter.isBlank()) {
            return true;
        }
        if (record == null) {
            return false;
        }
        return switch (selectedFilter) {
            case "RETURNED" -> true;
            case "LATE" -> record.getLateDays() != null && record.getLateDays() > 0;
            case "ON_TIME" -> record.getLateDays() == null || record.getLateDays() <= 0;
            case "TO_RETURN" -> false;
            default -> true;
        };
    }

    private boolean isPotentiallyLate(Reservation reservation) {
        if (reservation == null || reservation.getEndDate() == null) {
            return false;
        }
        return LocalDate.now(ZoneId.systemDefault()).isAfter(reservation.getEndDate());
    }
}
