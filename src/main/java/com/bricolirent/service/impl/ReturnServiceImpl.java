package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.ReturnRecord;
import com.bricolirent.repository.ReturnRecordRepository;
import com.bricolirent.service.ReturnService;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of ReturnService.
 */
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRecordRepository returnRecordRepository = new ReturnRecordRepository();

    @Override
    public List<ReturnRecord> getAllReturnRecords() {
        return returnRecordRepository.findAll();
    }

    @Override
    public Optional<ReturnRecord> getReturnRecordById(Long id) {
        return returnRecordRepository.findById(id);
    }

    @Override
    public Optional<ReturnRecord> getReturnByReservation(Long reservationId) {
        return returnRecordRepository.findByReservationId(reservationId);
    }

    @Override
    public void processReturn(ReturnRecord returnRecord) {
        // TODO: calculate late days and penalty based on reservation end date
        returnRecordRepository.save(returnRecord);
    }
}
