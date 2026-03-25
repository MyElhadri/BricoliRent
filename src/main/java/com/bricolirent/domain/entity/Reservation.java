package com.bricolirent.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "tool_id", nullable = false)
    private Tool tool;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "handled_by_agent_id")
    private Agent handledByAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "checkout_agent_id")
    private Agent checkoutAgent;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "reservation_date", nullable = false)
    private Instant reservationDate;

    @ColumnDefault("'PENDING'")
    @Column(name = "status", columnDefinition = "reservation_status not null")
    private Object status;

    @ColumnDefault("0")
    @Column(name = "estimated_rental_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedRentalAmount;

    @ColumnDefault("0")
    @Column(name = "estimated_deposit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedDepositAmount;

    @ColumnDefault("false")
    @Column(name = "approved_automatically", nullable = false)
    private Boolean approvedAutomatically;

    @Column(name = "approval_reason", length = Integer.MAX_VALUE)
    private String approvalReason;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public Agent getHandledByAgent() {
        return handledByAgent;
    }

    public void setHandledByAgent(Agent handledByAgent) {
        this.handledByAgent = handledByAgent;
    }

    public Agent getCheckoutAgent() {
        return checkoutAgent;
    }

    public void setCheckoutAgent(Agent checkoutAgent) {
        this.checkoutAgent = checkoutAgent;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Instant getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(Instant reservationDate) {
        this.reservationDate = reservationDate;
    }

    public Object getStatus() {
        return status;
    }

    public void setStatus(Object status) {
        this.status = status;
    }

    public BigDecimal getEstimatedRentalAmount() {
        return estimatedRentalAmount;
    }

    public void setEstimatedRentalAmount(BigDecimal estimatedRentalAmount) {
        this.estimatedRentalAmount = estimatedRentalAmount;
    }

    public BigDecimal getEstimatedDepositAmount() {
        return estimatedDepositAmount;
    }

    public void setEstimatedDepositAmount(BigDecimal estimatedDepositAmount) {
        this.estimatedDepositAmount = estimatedDepositAmount;
    }

    public Boolean getApprovedAutomatically() {
        return approvedAutomatically;
    }

    public void setApprovedAutomatically(Boolean approvedAutomatically) {
        this.approvedAutomatically = approvedAutomatically;
    }

    public String getApprovalReason() {
        return approvalReason;
    }

    public void setApprovalReason(String approvalReason) {
        this.approvalReason = approvalReason;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getCheckedOutAt() {
        return checkedOutAt;
    }

    public void setCheckedOutAt(Instant checkedOutAt) {
        this.checkedOutAt = checkedOutAt;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

}