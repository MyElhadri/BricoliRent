package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.ReturnRecord;
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
        addReservations(reservations, reservationRepository.findByStatusWithToolAndClient(ReservationStatus.APPROVED));
        addReservations(reservations, reservationRepository.findByStatusWithToolAndClient(ReservationStatus.CHECKED_OUT));
        addReservations(reservations, reservationRepository.findByStatusWithToolAndClient(ReservationStatus.RETURNED));

        List<PaymentCandidate> candidates = new ArrayList<>();
        for (Reservation reservation : reservations.values()) {
            List<Payment> payments = paymentRepository.findByReservationId(reservation.getId());
            boolean rentalPaid = containsPaymentType(payments, PaymentType.RENTAL);
            boolean depositPaid = containsPaymentType(payments, PaymentType.DEPOSIT);
            ReturnRecord returnRecord = returnRecordRepository.findByReservationId(reservation.getId());
            BigDecimal latePenalty = returnRecord == null || returnRecord.getLatePenalty() == null
                    ? BigDecimal.ZERO
                    : returnRecord.getLatePenalty();
            boolean latePenaltyPaid = containsPaymentType(payments, PaymentType.LATE_PENALTY);

            PaymentCandidate candidate = new PaymentCandidate(
                    reservation,
                    safeAmount(reservation.getEstimatedRentalAmount()),
                    safeAmount(reservation.getEstimatedDepositAmount()),
                    safeAmount(latePenalty),
                    rentalPaid,
                    depositPaid,
                    latePenaltyPaid
            );

            if (hasOutstandingPayment(candidate)) {
                candidates.add(candidate);
            }
        }
        return candidates;
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
        return payments.stream().anyMatch(payment -> payment.getType() == type);
    }

    private boolean hasOutstandingPayment(PaymentCandidate candidate) {
        boolean rentalDue = candidate.getRentalAmount().compareTo(BigDecimal.ZERO) > 0 && !candidate.isRentalPaid();
        boolean depositDue = candidate.getDepositAmount().compareTo(BigDecimal.ZERO) > 0 && !candidate.isDepositPaid();
        boolean penaltyDue = candidate.getLatePenaltyAmount().compareTo(BigDecimal.ZERO) > 0 && !candidate.isLatePenaltyPaid();
        return rentalDue || depositDue || penaltyDue;
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
            default -> throw new IllegalArgumentException("Type de paiement non pris en charge.");
        };
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String buildPaymentNotes(PaymentType type, BigDecimal amount) {
        return "Paiement cash enregistre pour " + type + " : " + amount + " EUR.";
    }
}
