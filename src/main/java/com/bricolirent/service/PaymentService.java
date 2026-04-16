package com.bricolirent.service;

import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.enums.PaymentType;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    List<PaymentCandidate> getPaymentCandidates();

    void enregistrerPaiementCash(Long reservationId, PaymentType type, BigDecimal amount, Long agentId);

    List<Payment> getPaymentHistoryByAgent(Long agentId);

    class PaymentCandidate {
        private final Reservation reservation;
        private final BigDecimal rentalAmount;
        private final BigDecimal depositAmount;
        private final BigDecimal latePenaltyAmount;
        private final BigDecimal refundAmount;
        private final boolean rentalPaid;
        private final boolean depositPaid;
        private final boolean latePenaltyPaid;
        private final boolean refundPaid;

        public PaymentCandidate(Reservation reservation,
                                BigDecimal rentalAmount,
                                BigDecimal depositAmount,
                                BigDecimal latePenaltyAmount,
                                BigDecimal refundAmount,
                                boolean rentalPaid,
                                boolean depositPaid,
                                boolean latePenaltyPaid,
                                boolean refundPaid) {
            this.reservation = reservation;
            this.rentalAmount = rentalAmount;
            this.depositAmount = depositAmount;
            this.latePenaltyAmount = latePenaltyAmount;
            this.refundAmount = refundAmount;
            this.rentalPaid = rentalPaid;
            this.depositPaid = depositPaid;
            this.latePenaltyPaid = latePenaltyPaid;
            this.refundPaid = refundPaid;
        }

        public Reservation getReservation() {
            return reservation;
        }

        public BigDecimal getRentalAmount() {
            return rentalAmount;
        }

        public BigDecimal getDepositAmount() {
            return depositAmount;
        }

        public BigDecimal getLatePenaltyAmount() {
            return latePenaltyAmount;
        }

        public BigDecimal getRefundAmount() {
            return refundAmount;
        }

        public boolean isRentalPaid() {
            return rentalPaid;
        }

        public boolean isDepositPaid() {
            return depositPaid;
        }

        public boolean isLatePenaltyPaid() {
            return latePenaltyPaid;
        }

        public boolean isRefundPaid() {
            return refundPaid;
        }
    }
}
