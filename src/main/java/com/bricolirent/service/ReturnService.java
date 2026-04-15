package com.bricolirent.service;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.ReturnRecord;

import java.math.BigDecimal;
import java.util.List;

public interface ReturnService {

    List<Reservation> getReservationsToReturn();

    ReturnProcessResult enregistrerRetour(Long reservationId, Long agentId);

    List<ReturnRecord> getReturnHistoryByAgent(Long agentId);

    record ReturnProcessResult(Long returnRecordId, int delayDays, BigDecimal latePenalty, String message) {
    }
}
