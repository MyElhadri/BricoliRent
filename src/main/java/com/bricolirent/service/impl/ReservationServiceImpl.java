package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.repository.ReservationRepository;
import com.bricolirent.service.ReservationService;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of ReservationService.
 */
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository = new ReservationRepository();

    @Override
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Override
    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    @Override
    public List<Reservation> getReservationsByClient(Long clientId) {
        return reservationRepository.findByClientId(clientId);
    }

    @Override
    public void createReservation(Reservation reservation) {
        // TODO: validate client score, check tool availability, calculate total price
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
    }

    @Override
    public void updateReservation(Reservation reservation) {
        reservationRepository.update(reservation);
    }

    @Override
    public void cancelReservation(Long reservationId) {
        Optional<Reservation> opt = reservationRepository.findById(reservationId);
        opt.ifPresent(reservation -> {
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.update(reservation);
        });
    }

    @Override
    public void checkoutReservation(Long reservationId, Long agentId) {
        // TODO: implement agent checkout logic
        Optional<Reservation> opt = reservationRepository.findById(reservationId);
        opt.ifPresent(reservation -> {
            reservation.setStatus(ReservationStatus.CHECKED_OUT);
            reservationRepository.update(reservation);
        });
    }
}
