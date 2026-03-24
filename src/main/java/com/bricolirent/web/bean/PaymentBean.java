package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Payment;
import com.bricolirent.service.PaymentService;
import com.bricolirent.service.impl.PaymentServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * JSF managed bean for payment tracking.
 */
@Named("paymentBean")
@RequestScoped
public class PaymentBean implements Serializable {

    private List<Payment> payments;
    private Payment selectedPayment;

    private final PaymentService paymentService = new PaymentServiceImpl();

    @PostConstruct
    public void init() {
        payments = paymentService.getAllPayments();
    }

    // ==================== Getters & Setters ====================

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public Payment getSelectedPayment() {
        return selectedPayment;
    }

    public void setSelectedPayment(Payment selectedPayment) {
        this.selectedPayment = selectedPayment;
    }
}
