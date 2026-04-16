package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.enums.PaymentStatus;
import com.bricolirent.domain.enums.PaymentType;
import com.bricolirent.service.PaymentService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Named("agentPaymentBean")
@ViewScoped
public class AgentPaymentBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AgentPaymentBean.class.getName());

    @Inject
    private PaymentService paymentService;

    @Inject
    private LoginBean loginBean;

    private List<PaymentRow> paymentRows = Collections.emptyList();
    private List<Payment> paymentHistory = Collections.emptyList();
    private String selectedFilter;
    private String searchKeyword;

    @PostConstruct
    public void init() {
        refreshData();
    }

    public void registerPayment(PaymentRow row) {
        try {
            paymentService.enregistrerPaiementCash(
                    row.getReservation().getId(),
                    row.getType(),
                    row.getAmount(),
                    getAgentId()
            );
            addMessage(
                    FacesMessage.SEVERITY_INFO,
                    row.isRefund() ? "Remboursement enregistre" : "Paiement enregistre",
                    row.isRefund()
                            ? "Le remboursement de caution a ete trace avec succes."
                            : "Le paiement cash a ete enregistre avec succes."
            );
            refreshData();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement d'un paiement", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
    }

    public String actionLabel(PaymentRow row) {
        return row != null && row.isRefund() ? "Enregistrer le remboursement" : "Enregistrer le paiement";
    }

    public String dueStatusLabel(PaymentRow row) {
        if (row == null) {
            return "Inconnu";
        }
        return switch (row.getType()) {
            case RENTAL -> "Location a encaisser";
            case DEPOSIT -> "Caution a encaisser";
            case LATE_PENALTY -> "Penalite a encaisser";
            case REFUND -> "Caution a rembourser";
        };
    }

    public String dueStatusTone(PaymentRow row) {
        if (row == null) {
            return "neutral";
        }
        return row.isRefund() ? "info" : "warning";
    }

    public String reservationStatusLabel(PaymentRow row) {
        if (row == null || row.getReservation() == null || row.getReservation().getStatus() == null) {
            return "Inconnu";
        }
        return switch (row.getReservation().getStatus()) {
            case APPROVED -> "Approuvee";
            case CHECKED_OUT -> "Check-out effectue";
            case RETURNED -> "Retour enregistre";
            case PENDING -> "En attente";
            case REJECTED -> "Rejetee";
        };
    }

    public String reservationStatusTone(PaymentRow row) {
        if (row == null || row.getReservation() == null || row.getReservation().getStatus() == null) {
            return "neutral";
        }
        return switch (row.getReservation().getStatus()) {
            case APPROVED -> "info";
            case CHECKED_OUT -> "warning";
            case RETURNED -> "success";
            case PENDING -> "warning";
            case REJECTED -> "danger";
        };
    }

    public String actionHint(PaymentRow row) {
        if (row == null) {
            return "";
        }
        return switch (row.getType()) {
            case RENTAL -> "Montant fixe de location calcule pour la reservation.";
            case DEPOSIT -> "Caution a encaisser avant ou juste apres la remise du materiel.";
            case LATE_PENALTY -> "Penalite de retard calculee automatiquement au retour.";
            case REFUND -> "Remboursement de caution disponible apres retour, si aucun blocage paiement ne subsiste.";
        };
    }

    public String paymentStatusLabel(Payment payment) {
        if (payment == null || payment.getStatus() == null) {
            return "Inconnu";
        }
        return payment.getStatus() == PaymentStatus.PAID ? "Paye" : "En attente";
    }

    public String paymentStatusTone(Payment payment) {
        if (payment == null || payment.getStatus() == null) {
            return "neutral";
        }
        return payment.getStatus() == PaymentStatus.PAID ? "success" : "warning";
    }

    public String typeLabel(PaymentType type) {
        if (type == null) {
            return "Inconnu";
        }
        return switch (type) {
            case RENTAL -> "Location";
            case DEPOSIT -> "Caution";
            case LATE_PENALTY -> "Penalite de retard";
            case REFUND -> "Remboursement";
        };
    }

    public List<PaymentRow> getPaymentRows() {
        return paymentRows;
    }

    public List<Payment> getPaymentHistory() {
        return paymentHistory;
    }

    public List<PaymentRow> getFilteredPaymentRows() {
        return paymentRows;
    }

    public List<Payment> getFilteredPaymentHistory() {
        return paymentHistory.stream()
                .filter(this::matchesHistorySearch)
                .filter(this::matchesPaidFilter)
                .collect(Collectors.toList());
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

    public String imageName(PaymentRow row) {
        return row == null ? "default-tool.jpg" : imageName(row.getReservation());
    }

    public String imageName(Payment payment) {
        return payment == null ? "default-tool.jpg" : imageName(payment.getReservation());
    }

    private void refreshData() {
        try {
            List<PaymentService.PaymentCandidate> paymentCandidates = paymentService.getPaymentCandidates();
            paymentRows = buildPaymentRows(paymentCandidates);
            Long agentId = getAgentId();
            paymentHistory = agentId == null
                    ? Collections.emptyList()
                    : paymentService.getPaymentHistoryByAgent(agentId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des donnees de paiement", e);
            paymentRows = Collections.emptyList();
            paymentHistory = Collections.emptyList();
        }
    }

    private List<PaymentRow> buildPaymentRows(List<PaymentService.PaymentCandidate> paymentCandidates) {
        List<PaymentRow> rows = new ArrayList<>();
        for (PaymentService.PaymentCandidate candidate : paymentCandidates) {
            if (!candidate.isLatePenaltyPaid() && candidate.getLatePenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
                rows.add(new PaymentRow(candidate.getReservation(), PaymentType.LATE_PENALTY, candidate.getLatePenaltyAmount()));
            }
            if (!candidate.isRefundPaid() && candidate.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
                rows.add(new PaymentRow(candidate.getReservation(), PaymentType.REFUND, candidate.getRefundAmount()));
            }
        }
        return rows;
    }

    private Long getAgentId() {
        return loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

    private String imageName(Reservation reservation) {
        if (reservation == null || reservation.getTool() == null || reservation.getTool().getImagePath() == null) {
            return "default-tool.jpg";
        }
        String imagePath = reservation.getTool().getImagePath().trim();
        return imagePath.isEmpty() ? "default-tool.jpg" : imagePath;
    }

    private boolean matchesRowSearch(PaymentRow row) {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return true;
        }
        if (row == null || row.getReservation() == null) {
            return false;
        }
        String keyword = searchKeyword.toLowerCase(Locale.ROOT).trim();
        String toolName = row.getReservation().getTool() != null && row.getReservation().getTool().getName() != null
                ? row.getReservation().getTool().getName().toLowerCase(Locale.ROOT) : "";
        String clientName = row.getReservation().getClient() != null
                && row.getReservation().getClient().getUsers() != null
                && row.getReservation().getClient().getUsers().getFullName() != null
                ? row.getReservation().getClient().getUsers().getFullName().toLowerCase(Locale.ROOT) : "";
        String reservationId = row.getReservation().getId() != null ? String.valueOf(row.getReservation().getId()) : "";
        return toolName.contains(keyword) || clientName.contains(keyword) || reservationId.contains(keyword);
    }

    private boolean matchesHistorySearch(Payment payment) {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return true;
        }
        if (payment == null || payment.getReservation() == null) {
            return false;
        }
        String keyword = searchKeyword.toLowerCase(Locale.ROOT).trim();
        String toolName = payment.getReservation().getTool() != null && payment.getReservation().getTool().getName() != null
                ? payment.getReservation().getTool().getName().toLowerCase(Locale.ROOT) : "";
        String clientName = payment.getReservation().getClient() != null
                && payment.getReservation().getClient().getUsers() != null
                && payment.getReservation().getClient().getUsers().getFullName() != null
                ? payment.getReservation().getClient().getUsers().getFullName().toLowerCase(Locale.ROOT) : "";
        String reservationId = payment.getReservation().getId() != null ? String.valueOf(payment.getReservation().getId()) : "";
        return toolName.contains(keyword) || clientName.contains(keyword) || reservationId.contains(keyword);
    }

    private boolean matchesDueFilter(PaymentRow row) {
        return true;
    }

    private boolean matchesPaidFilter(Payment payment) {
        if (selectedFilter == null || selectedFilter.isBlank()) {
            return true;
        }
        if (payment == null) {
            return false;
        }
        return switch (selectedFilter) {
            case "PAID" -> true;
            case "RENTAL" -> payment.getType() == PaymentType.RENTAL;
            case "DEPOSIT" -> payment.getType() == PaymentType.DEPOSIT;
            case "DUE" -> false;
            case "LATE_PENALTY" -> payment.getType() == PaymentType.LATE_PENALTY;
            case "REFUND" -> payment.getType() == PaymentType.REFUND;
            default -> true;
        };
    }

    public static class PaymentRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Reservation reservation;
        private final PaymentType type;
        private final BigDecimal amount;

        public PaymentRow(Reservation reservation, PaymentType type, BigDecimal amount) {
            this.reservation = reservation;
            this.type = type;
            this.amount = amount;
        }

        public Reservation getReservation() {
            return reservation;
        }

        public PaymentType getType() {
            return type;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public boolean isRefund() {
            return type == PaymentType.REFUND;
        }
    }
}
