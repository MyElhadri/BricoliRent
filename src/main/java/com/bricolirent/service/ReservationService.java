package com.bricolirent.service;

import com.bricolirent.domain.entity.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReservationService {

    class ReservationCreationResult {
        private final Long reservationId;
        private final com.bricolirent.domain.enums.ReservationStatus status;
        private final String message;
        private final String reason;

        public ReservationCreationResult(Long reservationId,
                                         com.bricolirent.domain.enums.ReservationStatus status,
                                         String message,
                                         String reason) {
            this.reservationId = reservationId;
            this.status = status;
            this.message = message;
            this.reason = reason;
        }

        public Long getReservationId() {
            return reservationId;
        }

        public com.bricolirent.domain.enums.ReservationStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public String getReason() {
            return reason;
        }
    }

    class ReservationEstimate {
        private final boolean available;
        private final String message;
        private final long rentalDays;
        private final BigDecimal estimatedRentalAmount;
        private final BigDecimal estimatedDepositAmount;

        public ReservationEstimate(boolean available, String message, long rentalDays,
                                   BigDecimal estimatedRentalAmount, BigDecimal estimatedDepositAmount) {
            this.available = available;
            this.message = message;
            this.rentalDays = rentalDays;
            this.estimatedRentalAmount = estimatedRentalAmount;
            this.estimatedDepositAmount = estimatedDepositAmount;
        }

        public boolean isAvailable() {
            return available;
        }

        public String getMessage() {
            return message;
        }

        public long getRentalDays() {
            return rentalDays;
        }

        public BigDecimal getEstimatedRentalAmount() {
            return estimatedRentalAmount;
        }

        public BigDecimal getEstimatedDepositAmount() {
            return estimatedDepositAmount;
        }
    }

    ReservationEstimate estimerDemande(Long toolId, int quantity, LocalDate startDate, LocalDate endDate);

    ReservationCreationResult creerDemande(Long clientId, Long toolId, int quantity, LocalDate startDate, LocalDate endDate);

    List<Reservation> getReservationsByClient(Long clientId);

    Reservation getReservationForClient(Long reservationId, Long clientId);

    void annulerDemande(Long reservationId, Long clientId);

    List<Reservation> getPendingReservations();

    List<Reservation> getHandledReservationsByAgent(Long agentId);

    void approuverDemande(Long reservationId, Long agentId);

    void rejeterDemande(Long reservationId, Long agentId, String reason);
}
