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
import java.util.logging.Level;
import java.util.logging.Logger;

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
