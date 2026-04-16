package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.enums.PaymentStatus;
import com.bricolirent.domain.enums.PaymentType;
import com.bricolirent.service.PaymentService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private List<PaymentService.PaymentCandidate> paymentCandidates = Collections.emptyList();
    private List<Payment> paymentHistory = Collections.emptyList();
    private Map<Long, PaymentType> selectedTypes = new HashMap<>();
    private Map<Long, BigDecimal> enteredAmounts = new HashMap<>();

    @PostConstruct
    public void init() {
        refreshData();
    }

    public void registerPayment(Long reservationId) {
        try {
            paymentService.enregistrerPaiementCash(
                    reservationId,
                    selectedTypes.get(reservationId),
                    enteredAmounts.get(reservationId),
                    getAgentId()
            );
            addMessage(FacesMessage.SEVERITY_INFO, "Paiement enregistre", "Le paiement cash a ete enregistre avec succes.");
            refreshData();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement d'un paiement", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
    }

    public List<SelectItem> getTypeOptions(PaymentService.PaymentCandidate candidate) {
        List<SelectItem> items = new ArrayList<>();
        if (!candidate.isRentalPaid() && candidate.getRentalAmount().compareTo(BigDecimal.ZERO) > 0) {
            items.add(new SelectItem(PaymentType.RENTAL, "Location"));
        }
        if (!candidate.isDepositPaid() && candidate.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            items.add(new SelectItem(PaymentType.DEPOSIT, "Caution"));
        }
        if (!candidate.isLatePenaltyPaid() && candidate.getLatePenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
            items.add(new SelectItem(PaymentType.LATE_PENALTY, "Penalite de retard"));
        }
        return items;
    }

    public List<SelectItem> typeOptions(PaymentService.PaymentCandidate candidate) {
        return getTypeOptions(candidate);
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

    public List<PaymentService.PaymentCandidate> getPaymentCandidates() {
        return paymentCandidates;
    }

    public List<Payment> getPaymentHistory() {
        return paymentHistory;
    }

    public Map<Long, PaymentType> getSelectedTypes() {
        return selectedTypes;
    }

    public Map<Long, BigDecimal> getEnteredAmounts() {
        return enteredAmounts;
    }

    private void refreshData() {
        try {
            paymentCandidates = paymentService.getPaymentCandidates();
            Long agentId = getAgentId();
            paymentHistory = agentId == null
                    ? Collections.emptyList()
                    : paymentService.getPaymentHistoryByAgent(agentId);
            initializeFormState();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des donnees de paiement", e);
            paymentCandidates = Collections.emptyList();
            paymentHistory = Collections.emptyList();
            selectedTypes = new HashMap<>();
            enteredAmounts = new HashMap<>();
        }
    }

    private void initializeFormState() {
        Map<Long, PaymentType> newSelectedTypes = new HashMap<>();
        Map<Long, BigDecimal> newEnteredAmounts = new HashMap<>();
        for (PaymentService.PaymentCandidate candidate : paymentCandidates) {
            PaymentType defaultType = defaultType(candidate);
            if (defaultType != null) {
                newSelectedTypes.put(candidate.getReservation().getId(), defaultType);
                newEnteredAmounts.put(candidate.getReservation().getId(), suggestedAmount(candidate, defaultType));
            }
        }
        selectedTypes = newSelectedTypes;
        enteredAmounts = newEnteredAmounts;
    }

    private PaymentType defaultType(PaymentService.PaymentCandidate candidate) {
        if (!candidate.isRentalPaid() && candidate.getRentalAmount().compareTo(BigDecimal.ZERO) > 0) {
            return PaymentType.RENTAL;
        }
        if (!candidate.isDepositPaid() && candidate.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            return PaymentType.DEPOSIT;
        }
        if (!candidate.isLatePenaltyPaid() && candidate.getLatePenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
            return PaymentType.LATE_PENALTY;
        }
        return null;
    }

    private BigDecimal suggestedAmount(PaymentService.PaymentCandidate candidate, PaymentType type) {
        return switch (type) {
            case RENTAL -> candidate.getRentalAmount();
            case DEPOSIT -> candidate.getDepositAmount();
            case LATE_PENALTY -> candidate.getLatePenaltyAmount();
            case REFUND -> BigDecimal.ZERO;
        };
    }

    private Long getAgentId() {
        return loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
