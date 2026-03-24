package com.bricolirent.service;

import com.bricolirent.domain.entity.ReturnRecord;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing tool returns.
 */
public interface ReturnService {

    List<ReturnRecord> getAllReturnRecords();

    Optional<ReturnRecord> getReturnRecordById(Long id);

    Optional<ReturnRecord> getReturnByReservation(Long reservationId);

    void processReturn(ReturnRecord returnRecord);
}
