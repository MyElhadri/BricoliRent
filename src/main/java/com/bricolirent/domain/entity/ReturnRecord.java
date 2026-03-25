package com.bricolirent.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "return_records")
public class ReturnRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "handled_by_agent_id")
    private Agent handledByAgent;

    @Column(name = "actual_return_date", nullable = false)
    private Instant actualReturnDate;

    @ColumnDefault("0")
    @Column(name = "late_days", nullable = false)
    private Integer lateDays;

    @ColumnDefault("0")
    @Column(name = "late_penalty", nullable = false, precision = 10, scale = 2)
    private BigDecimal latePenalty;

    @Column(name = "notes", length = Integer.MAX_VALUE)
    private String notes;

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

    public Agent getHandledByAgent() {
        return handledByAgent;
    }

    public void setHandledByAgent(Agent handledByAgent) {
        this.handledByAgent = handledByAgent;
    }

    public Instant getActualReturnDate() {
        return actualReturnDate;
    }

    public void setActualReturnDate(Instant actualReturnDate) {
        this.actualReturnDate = actualReturnDate;
    }

    public Integer getLateDays() {
        return lateDays;
    }

    public void setLateDays(Integer lateDays) {
        this.lateDays = lateDays;
    }

    public BigDecimal getLatePenalty() {
        return latePenalty;
    }

    public void setLatePenalty(BigDecimal latePenalty) {
        this.latePenalty = latePenalty;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

}