package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Agent;
import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.enums.PaymentStatus;
import com.bricolirent.domain.enums.PaymentType;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.service.AdminSupervisionService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Named("adminSupervisionBean")
@ViewScoped
public class AdminSupervisionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AdminSupervisionService adminSupervisionService;

    private List<Reservation> reservations = Collections.emptyList();
    private List<Payment> payments = Collections.emptyList();

    private String reservationSearchKeyword;
    private String reservationStatusFilter;
    private String paymentSearchKeyword;
    private String paymentTypeFilter;
    private String paymentStatusFilter;

    @PostConstruct
    public void init() {
        reservations = adminSupervisionService.getAllReservations();
        payments = adminSupervisionService.getAllPayments();
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public List<Reservation> getFilteredReservations() {
        return reservations.stream()
                .filter(this::matchesReservationSearch)
                .filter(this::matchesReservationStatus)
                .collect(Collectors.toList());
    }

    public List<Payment> getFilteredPayments() {
        return payments.stream()
                .filter(this::matchesPaymentSearch)
                .filter(this::matchesPaymentType)
                .filter(this::matchesPaymentStatus)
                .collect(Collectors.toList());
    }

    public void applyReservationFilters() {
        // filtrage calcule a la demande
    }

    public void applyPaymentFilters() {
        // filtrage calcule a la demande
    }

    public boolean reservationStatusFilterActive(String filterKey) {
        if (filterKey == null || filterKey.isBlank()) {
            return reservationStatusFilter == null || reservationStatusFilter.isBlank();
        }
        return filterKey.equalsIgnoreCase(reservationStatusFilter);
    }

    public boolean paymentTypeFilterActive(String filterKey) {
        if (filterKey == null || filterKey.isBlank()) {
            return paymentTypeFilter == null || paymentTypeFilter.isBlank();
        }
        return filterKey.equalsIgnoreCase(paymentTypeFilter);
    }

    public boolean paymentStatusFilterActive(String filterKey) {
        if (filterKey == null || filterKey.isBlank()) {
            return paymentStatusFilter == null || paymentStatusFilter.isBlank();
        }
        return filterKey.equalsIgnoreCase(paymentStatusFilter);
    }

    public String reservationStatusLabel(Reservation reservation) {
        if (reservation == null || reservation.getStatus() == null) {
            return "Inconnu";
        }
        return switch (reservation.getStatus()) {
            case PENDING -> "En attente";
            case APPROVED -> "Approuvee";
            case REJECTED -> "Rejetee";
            case CHECKED_OUT -> "Check-out effectue";
            case RETURNED -> "Retournee";
        };
    }

    public String reservationStatusTone(Reservation reservation) {
        if (reservation == null || reservation.getStatus() == null) {
            return "neutral";
        }
        return switch (reservation.getStatus()) {
            case PENDING -> "warning";
            case APPROVED -> "info";
            case REJECTED -> "danger";
            case CHECKED_OUT, RETURNED -> "success";
        };
    }

    public String paymentTypeLabel(PaymentType type) {
        if (type == null) {
            return "Inconnu";
        }
        return switch (type) {
            case RENTAL -> "Location";
            case DEPOSIT -> "Caution";
            case LATE_PENALTY -> "Penalite";
            case REFUND -> "Remboursement";
        };
    }

    public String paymentTypeTone(PaymentType type) {
        if (type == null) {
            return "neutral";
        }
        return switch (type) {
            case RENTAL -> "info";
            case DEPOSIT -> "warning";
            case LATE_PENALTY -> "danger";
            case REFUND -> "success";
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

    public String paymentMethodLabel(Payment payment) {
        return payment == null || payment.getMethod() == null ? "Inconnue" : payment.getMethod().name();
    }

    public String agentLabel(Agent agent) {
        if (agent == null || agent.getUsers() == null || agent.getUsers().getFullName() == null || agent.getUsers().getFullName().isBlank()) {
            return "Non renseigne";
        }
        return agent.getUsers().getFullName();
    }

    public String imageName(Reservation reservation) {
        if (reservation == null || reservation.getTool() == null || reservation.getTool().getImagePath() == null) {
            return "default-tool.jpg";
        }
        String imagePath = reservation.getTool().getImagePath().trim();
        return imagePath.isEmpty() ? "default-tool.jpg" : imagePath;
    }

    public String imageName(Payment payment) {
        return payment == null ? "default-tool.jpg" : imageName(payment.getReservation());
    }

    public String getReservationSearchKeyword() {
        return reservationSearchKeyword;
    }

    public void setReservationSearchKeyword(String reservationSearchKeyword) {
        this.reservationSearchKeyword = reservationSearchKeyword;
    }

    public String getReservationStatusFilter() {
        return reservationStatusFilter;
    }

    public void setReservationStatusFilter(String reservationStatusFilter) {
        this.reservationStatusFilter = reservationStatusFilter;
    }

    public String getPaymentSearchKeyword() {
        return paymentSearchKeyword;
    }

    public void setPaymentSearchKeyword(String paymentSearchKeyword) {
        this.paymentSearchKeyword = paymentSearchKeyword;
    }

    public String getPaymentTypeFilter() {
        return paymentTypeFilter;
    }

    public void setPaymentTypeFilter(String paymentTypeFilter) {
        this.paymentTypeFilter = paymentTypeFilter;
    }

    public String getPaymentStatusFilter() {
        return paymentStatusFilter;
    }

    public void setPaymentStatusFilter(String paymentStatusFilter) {
        this.paymentStatusFilter = paymentStatusFilter;
    }

    private boolean matchesReservationSearch(Reservation reservation) {
        if (reservationSearchKeyword == null || reservationSearchKeyword.isBlank()) {
            return true;
        }
        if (reservation == null) {
            return false;
        }
        String keyword = reservationSearchKeyword.trim().toLowerCase(Locale.ROOT);
        String reservationId = reservation.getId() == null ? "" : String.valueOf(reservation.getId());
        String toolName = reservation.getTool() != null && reservation.getTool().getName() != null
                ? reservation.getTool().getName().toLowerCase(Locale.ROOT)
                : "";
        String clientName = reservation.getClient() != null
                && reservation.getClient().getUsers() != null
                && reservation.getClient().getUsers().getFullName() != null
                ? reservation.getClient().getUsers().getFullName().toLowerCase(Locale.ROOT)
                : "";
        return reservationId.contains(keyword) || toolName.contains(keyword) || clientName.contains(keyword);
    }

    private boolean matchesReservationStatus(Reservation reservation) {
        if (reservationStatusFilter == null || reservationStatusFilter.isBlank()) {
            return true;
        }
        return reservation != null
                && reservation.getStatus() != null
                && reservation.getStatus().name().equalsIgnoreCase(reservationStatusFilter);
    }

    private boolean matchesPaymentSearch(Payment payment) {
        if (paymentSearchKeyword == null || paymentSearchKeyword.isBlank()) {
            return true;
        }
        if (payment == null || payment.getReservation() == null) {
            return false;
        }
        String keyword = paymentSearchKeyword.trim().toLowerCase(Locale.ROOT);
        String paymentId = payment.getId() == null ? "" : String.valueOf(payment.getId());
        String reservationId = payment.getReservation().getId() == null ? "" : String.valueOf(payment.getReservation().getId());
        String toolName = payment.getReservation().getTool() != null && payment.getReservation().getTool().getName() != null
                ? payment.getReservation().getTool().getName().toLowerCase(Locale.ROOT)
                : "";
        String clientName = payment.getReservation().getClient() != null
                && payment.getReservation().getClient().getUsers() != null
                && payment.getReservation().getClient().getUsers().getFullName() != null
                ? payment.getReservation().getClient().getUsers().getFullName().toLowerCase(Locale.ROOT)
                : "";
        return paymentId.contains(keyword)
                || reservationId.contains(keyword)
                || toolName.contains(keyword)
                || clientName.contains(keyword);
    }

    private boolean matchesPaymentType(Payment payment) {
        if (paymentTypeFilter == null || paymentTypeFilter.isBlank()) {
            return true;
        }
        return payment != null
                && payment.getType() != null
                && payment.getType().name().equalsIgnoreCase(paymentTypeFilter);
    }

    private boolean matchesPaymentStatus(Payment payment) {
        if (paymentStatusFilter == null || paymentStatusFilter.isBlank()) {
            return true;
        }
        return payment != null
                && payment.getStatus() != null
                && payment.getStatus().name().equalsIgnoreCase(paymentStatusFilter);
    }
}
