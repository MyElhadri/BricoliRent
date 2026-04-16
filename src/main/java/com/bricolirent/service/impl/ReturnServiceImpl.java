package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.ReturnRecord;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.repository.ReservationRepository;
import com.bricolirent.repository.ReturnRecordRepository;
import com.bricolirent.service.ReturnService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Named("returnService")
@ApplicationScoped
public class ReturnServiceImpl implements ReturnService {

    private static final BigDecimal LATE_FEE_PER_DAY = new BigDecimal("50.00");

    private ReservationRepository reservationRepository;
    private ReturnRecordRepository returnRecordRepository;

    @PostConstruct
    public void init() {
        this.reservationRepository = new ReservationRepository();
        this.returnRecordRepository = new ReturnRecordRepository();
    }

    @Override
    public List<Reservation> getReservationsToReturn() {
        return reservationRepository.findByStatusWithToolAndClient(ReservationStatus.CHECKED_OUT);
    }

    @Override
    public ReturnProcessResult enregistrerRetour(Long reservationId, Long agentId) {
        Reservation reservation = reservationRepository.findByIdWithToolAndClient(reservationId)
                .orElseThrow(() -> new IllegalStateException("La reservation selectionnee est introuvable."));

        if (reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new IllegalStateException("Seules les reservations deja sorties peuvent etre retournees.");
        }

        Instant actualReturnDate = Instant.now();
        LocalDate plannedEndDate = reservation.getEndDate();
        if (plannedEndDate == null) {
            plannedEndDate = actualReturnDate.atZone(ZoneId.systemDefault()).toLocalDate();
        }

        LocalDate actualReturnLocalDate = actualReturnDate.atZone(ZoneId.systemDefault()).toLocalDate();
        int delayDays = Math.max(0, (int) ChronoUnit.DAYS.between(plannedEndDate, actualReturnLocalDate));
        BigDecimal latePenalty = LATE_FEE_PER_DAY.multiply(BigDecimal.valueOf(delayDays));

        ReturnRecord record = returnRecordRepository.registerReturn(
                reservationId,
                agentId,
                actualReturnDate,
                delayDays,
                latePenalty,
                buildNotes(delayDays, latePenalty)
        );

        return new ReturnProcessResult(
                record.getId(),
                delayDays,
                latePenalty,
                buildClientMessage(delayDays, latePenalty)
        );
    }

    @Override
    public List<ReturnRecord> getReturnHistoryByAgent(Long agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("Agent invalide.");
        }
        return returnRecordRepository.findByHandledByAgent(agentId);
    }

    private String buildNotes(int delayDays, BigDecimal latePenalty) {
        if (delayDays <= 0) {
            return "Retour enregistre sans retard.";
        }
        return "Retour enregistre avec " + delayDays + " jour(s) de retard. Penalite calculee : " + latePenalty + " EUR.";
    }

    private String buildClientMessage(int delayDays, BigDecimal latePenalty) {
        if (delayDays <= 0) {
            return "Le retour a ete enregistre sans retard.";
        }
        return "Le retour a ete enregistre avec " + delayDays + " jour(s) de retard. Penalite de retard : " + latePenalty + " EUR.";
    }
}
