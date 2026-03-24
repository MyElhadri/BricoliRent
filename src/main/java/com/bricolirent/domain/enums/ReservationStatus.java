package com.bricolirent.domain.enums;

/**
 * Status of a reservation throughout its lifecycle.
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_OUT,
    RETURNED,
    CANCELLED,
    OVERDUE
}
