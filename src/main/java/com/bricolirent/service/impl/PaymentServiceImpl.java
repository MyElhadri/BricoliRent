package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.ReturnRecord;
import com.bricolirent.domain.enums.PaymentStatus;
import com.bricolirent.domain.enums.PaymentType;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.repository.PaymentRepository;
import com.bricolirent.repository.ReservationRepository;
import com.bricolirent.repository.ReturnRecordRepository;
import com.bricolirent.service.PaymentService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Named("paymentService")
@ApplicationScoped
public class PaymentServiceImpl implements PaymentService {

    private PaymentRepository paymentRepository;
    private ReservationRepository reservationRepository;
    private ReturnRecordRepository returnRecordRepository;

    @PostConstruct
    public void init() {
        this.paymentRepository = new PaymentRepository();
        this.reservationRepository = new ReservationRepository();
        this.returnRecordRepository = new ReturnRecordRepository();
    }

    @Override
    public List<PaymentCandidate> getPaymentCandidates() {
        Map<Long, Reservation> reservations = new LinkedHashMap<>();
        addReservations(reservations, reservationRepository.findByStatusWithToolAndClient(ReservationStatus.RETURNED));
        addReservations(reservations, reservationRepository.findByStatusWithToolAndClient(ReservationStatus.CHECKED_OUT));

        List<PaymentCandidate> candidates = new ArrayList<>();
        for (Reservation reservation : reservations.values()) {
            List<Payment> payments = paymentRepository.findByReservationId(reservation.getId());
            boolean rentalPaid = containsPaymentType(payments, PaymentType.RENTAL);
            boolean depositPaid = containsPaymentType(payments, PaymentType.DEPOSIT);
            boolean refundPaid = containsPaymentType(payments, PaymentType.REFUND);
            ReturnRecord returnRecord = returnRecordRepository.findByReservationId(reservation.getId());
            BigDecimal latePenalty = returnRecord == null || returnRecord.getLatePenalty() == null
                    ? BigDecimal.ZERO
                    : returnRecord.getLatePenalty();
            boolean latePenaltyPaid = containsPaymentType(payments, PaymentType.LATE_PENALTY);
            BigDecimal refundAmount = computeRefundAmount(reservation, returnRecord, depositPaid, latePenaltyPaid, refundPaid);

            PaymentCandidate candidate = new PaymentCandidate(
                    reservation,
                    safeAmount(reservation.getEstimatedRentalAmount()),
                    safeAmount(reservation.getEstimatedDepositAmount()),
                    safeAmount(latePenalty),
                    safeAmount(refundAmount),
                    rentalPaid,
                    depositPaid,
                    latePenaltyPaid,
                    refundPaid
            );

            if (hasOutstandingPayment(candidate)) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    @Override
    public void encaisserAvantCheckout(Long reservationId, Long agentId) {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation invalide.");
        }

        Reservation reservation = reservationRepository.findByIdWithToolAndClient(reservationId)
                .orElseThrow(() -> new IllegalStateException("La reservation selectionnee est introuvable."));

        if (reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new IllegalStateException("Seules les reservations approuvees peuvent etre encaissees avant check-out.");
        }

        BigDecimal rentalAmount = safeAmount(reservation.getEstimatedRentalAmount());
        BigDecimal depositAmount = safeAmount(reservation.getEstimatedDepositAmount());
        if (rentalAmount.compareTo(BigDecimal.ZERO) <= 0 || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("La location et la caution doivent etre definies avant l'encaissement.");
        }

        List<Payment> payments = paymentRepository.findByReservationId(reservationId);
        if (containsPaymentType(payments, PaymentType.RENTAL) || containsPaymentType(payments, PaymentType.DEPOSIT)) {
            throw new IllegalStateException("L'encaissement avant check-out a deja ete enregistre pour cette reservation.");
        }

        String receiptNumber = buildReceiptNumber("ENC", reservationId);
        paymentRepository.saveBeforeCheckoutPayments(
                reservationId,
                agentId,
                rentalAmount,
                depositAmount,
                receiptNumber,
                "Encaissement cash avant check-out - location : " + rentalAmount + " MAD.",
                "Encaissement cash avant check-out - caution : " + depositAmount + " MAD."
        );
    }

    @Override
    public boolean isPreCheckoutPaymentComplete(Long reservationId) {
        if (reservationId == null) {
            return false;
        }
        List<Payment> payments = paymentRepository.findByReservationId(reservationId);
        return containsPaymentType(payments, PaymentType.RENTAL)
                && containsPaymentType(payments, PaymentType.DEPOSIT);
    }

    @Override
    public void enregistrerPaiementCash(Long reservationId, PaymentType type, BigDecimal amount, Long agentId) {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation invalide.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Selectionnez un type de paiement.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit etre strictement positif.");
        }

        Reservation reservation = reservationRepository.findByIdWithToolAndClient(reservationId)
                .orElseThrow(() -> new IllegalStateException("La reservation selectionnee est introuvable."));

        BigDecimal expectedAmount = getExpectedAmount(reservation, type);
        if (expectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Aucun montant n'est attendu pour ce mouvement.");
        }
        if (amount.compareTo(expectedAmount) != 0) {
            throw new IllegalArgumentException("Le montant saisi doit correspondre au montant attendu : " + expectedAmount + " EUR.");
        }

        String notes = buildPaymentNotes(type, expectedAmount);
        paymentRepository.saveCashPayment(reservationId, agentId, type, expectedAmount, notes);
    }

    @Override
    public List<Payment> getPaymentHistoryByAgent(Long agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("Agent invalide.");
        }
        return paymentRepository.findByRecordedByAgent(agentId);
    }

    private void addReservations(Map<Long, Reservation> target, List<Reservation> source) {
        for (Reservation reservation : source) {
            target.putIfAbsent(reservation.getId(), reservation);
        }
    }

    private boolean containsPaymentType(List<Payment> payments, PaymentType type) {
        return payments.stream().anyMatch(payment -> payment.getType() == type && payment.getStatus() == PaymentStatus.PAID);
    }

    private boolean hasOutstandingPayment(PaymentCandidate candidate) {
        boolean rentalDue = candidate.getRentalAmount().compareTo(BigDecimal.ZERO) > 0 && !candidate.isRentalPaid();
        boolean depositDue = candidate.getDepositAmount().compareTo(BigDecimal.ZERO) > 0 && !candidate.isDepositPaid();
        boolean penaltyDue = candidate.getLatePenaltyAmount().compareTo(BigDecimal.ZERO) > 0 && !candidate.isLatePenaltyPaid();
        boolean refundDue = candidate.getRefundAmount().compareTo(BigDecimal.ZERO) > 0 && !candidate.isRefundPaid();
        return rentalDue || depositDue || penaltyDue || refundDue;
    }

    private BigDecimal getExpectedAmount(Reservation reservation, PaymentType type) {
        return switch (type) {
            case RENTAL -> safeAmount(reservation.getEstimatedRentalAmount());
            case DEPOSIT -> safeAmount(reservation.getEstimatedDepositAmount());
            case LATE_PENALTY -> {
                ReturnRecord returnRecord = returnRecordRepository.findByReservationId(reservation.getId());
                if (returnRecord == null || returnRecord.getLatePenalty() == null || returnRecord.getLatePenalty().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException("Aucune penalite de retard n'est due pour cette reservation.");
                }
                yield safeAmount(returnRecord.getLatePenalty());
            }
            case REFUND -> {
                List<Payment> payments = paymentRepository.findByReservationId(reservation.getId());
                boolean depositPaid = containsPaymentType(payments, PaymentType.DEPOSIT);
                boolean refundPaid = containsPaymentType(payments, PaymentType.REFUND);
                ReturnRecord returnRecord = returnRecordRepository.findByReservationId(reservation.getId());
                boolean latePenaltyPaid = containsPaymentType(payments, PaymentType.LATE_PENALTY);

                BigDecimal refundAmount = computeRefundAmount(reservation, returnRecord, depositPaid, latePenaltyPaid, refundPaid);
                if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException("La caution ne peut pas etre remboursee pour cette reservation.");
                }
                yield refundAmount;
            }
        };
    }

    private BigDecimal computeRefundAmount(Reservation reservation,
                                           ReturnRecord returnRecord,
                                           boolean depositPaid,
                                           boolean latePenaltyPaid,
                                           boolean refundPaid) {
        if (reservation.getStatus() != ReservationStatus.RETURNED) {
            return BigDecimal.ZERO;
        }
        if (!depositPaid || refundPaid) {
            return BigDecimal.ZERO;
        }

        BigDecimal latePenalty = returnRecord == null || returnRecord.getLatePenalty() == null
                ? BigDecimal.ZERO
                : safeAmount(returnRecord.getLatePenalty());
        if (latePenalty.compareTo(BigDecimal.ZERO) > 0 && !latePenaltyPaid) {
            return BigDecimal.ZERO;
        }

        return safeAmount(reservation.getEstimatedDepositAmount());
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String buildPaymentNotes(PaymentType type, BigDecimal amount) {
        return switch (type) {
            case RENTAL -> "Paiement cash enregistre pour la location : " + amount + " MAD.";
            case DEPOSIT -> "Paiement cash enregistre pour la caution : " + amount + " MAD.";
            case LATE_PENALTY -> "Paiement cash enregistre pour la penalite de retard : " + amount + " MAD.";
            case REFUND -> "Remboursement cash de la caution enregistre : " + amount + " MAD.";
        };
    }

    private String buildReceiptNumber(String prefix, Long reservationId) {
        return prefix + "-" + reservationId + "-" + System.currentTimeMillis();
    }
}
