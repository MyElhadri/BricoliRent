package com.bricolirent.service;

import com.bricolirent.domain.entity.Payment;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for payment tracking.
 */
public interface PaymentService {

    List<Payment> getAllPayments();

    Optional<Payment> getPaymentById(Long id);

    List<Payment> getPaymentsByReservation(Long reservationId);

    void createPayment(Payment payment);

    void updatePayment(Payment payment);
}
