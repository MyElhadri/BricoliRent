package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.service.ReservationService;
import com.bricolirent.service.impl.ReservationServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * JSF managed bean for reservation operations.
 */
@Named("reservationBean")
@RequestScoped
public class ReservationBean implements Serializable {

    private List<Reservation> reservations;
    private Reservation selectedReservation;
    private Reservation newReservation = new Reservation();

    private final ReservationService reservationService = new ReservationServiceImpl();

    @PostConstruct
    public void init() {
        reservations = reservationService.getAllReservations();
    }

    /**
     * Create a new reservation.
     */
    public String createReservation() {
        // TODO: set client, tool, compute total price
        reservationService.createReservation(newReservation);
        return "/app/dashboard.xhtml?faces-redirect=true";
    }

    // ==================== Getters & Setters ====================

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public Reservation getSelectedReservation() {
        return selectedReservation;
    }

    public void setSelectedReservation(Reservation selectedReservation) {
        this.selectedReservation = selectedReservation;
    }

    public Reservation getNewReservation() {
        return newReservation;
    }

    public void setNewReservation(Reservation newReservation) {
        this.newReservation = newReservation;
    }
}
