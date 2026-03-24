package com.bricolirent.domain.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Client entity — a user who can rent tools.
 * Has a score/points system for reservation validation.
 */
@Entity
@DiscriminatorValue("CLIENT")
public class Client extends User {

    @Column(name = "score")
    private int score = 100;

    @Column(name = "address")
    private String address;

    @Column(name = "cin", unique = true)
    private String cin;

    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    // ==================== Constructors ====================

    public Client() {
    }

    // ==================== Getters & Setters ====================

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }
}
