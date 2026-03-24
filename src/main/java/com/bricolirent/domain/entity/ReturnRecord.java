package com.bricolirent.domain.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ReturnRecord entity — records the return of tools from a reservation.
 * Tracks actual return date, late days, and penalty amount.
 * V1: only late penalties (no damage tracking).
 */
@Entity
@Table(name = "return_records")
public class ReturnRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "late_days")
    private int lateDays;

    @Column(name = "penalty_amount", precision = 10, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ==================== Constructors ====================

    public ReturnRecord() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ==================== Getters & Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public int getLateDays() {
        return lateDays;
    }

    public void setLateDays(int lateDays) {
        this.lateDays = lateDays;
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount;
    }

    public void setPenaltyAmount(BigDecimal penaltyAmount) {
        this.penaltyAmount = penaltyAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
