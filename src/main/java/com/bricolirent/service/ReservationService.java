package com.bricolirent.service;

import com.bricolirent.domain.entity.Reservation;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for reservation management.
 */
public interface ReservationService {

    List<Reservation> getAllReservations();

    Optional<Reservation> getReservationById(Long id);

    List<Reservation> getReservationsByClient(Long clientId);

    void createReservation(Reservation reservation);

    void updateReservation(Reservation reservation);

    void cancelReservation(Long reservationId);

    void checkoutReservation(Long reservationId, Long agentId);
}
