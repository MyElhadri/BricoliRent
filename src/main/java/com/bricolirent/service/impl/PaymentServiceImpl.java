package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Payment;
import com.bricolirent.repository.PaymentRepository;
import com.bricolirent.service.PaymentService;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of PaymentService.
 */
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository = new PaymentRepository();

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    @Override
    public List<Payment> getPaymentsByReservation(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId);
    }

    @Override
    public void createPayment(Payment payment) {
        // TODO: validate payment and update reservation status
        paymentRepository.save(payment);
    }

    @Override
    public void updatePayment(Payment payment) {
        paymentRepository.update(payment);
    }
}
