package com.bricolirent.repository;

import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.enums.PaymentMethod;
import com.bricolirent.domain.enums.PaymentStatus;
import com.bricolirent.domain.enums.PaymentType;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository pour l'entité Payment.
 * Fournit les opérations CRUD héritées + recherche par réservation.
 */
public class PaymentRepository extends GenericRepository<Payment, Long> {

    private static final Logger LOGGER = Logger.getLogger(PaymentRepository.class.getName());

    public PaymentRepository() {
        super(Payment.class);
    }

    /**
     * Récupère tous les paiements associés à une réservation donnée.
     * Une réservation peut avoir plusieurs paiements (location, caution, pénalité...).
     *
     * @param reservationId L'identifiant de la réservation
     * @return La liste des paiements de cette réservation
     */
    public List<Payment> findByReservationId(Long reservationId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Payment> payments = session
                    .createQuery(
                            "FROM Payment p WHERE p.reservation.id = :reservationId ORDER BY p.paymentDate DESC",
                            Payment.class)
                    .setParameter("reservationId", reservationId)
                    .getResultList();
            transaction.commit();
            return payments;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche des paiements pour la réservation ID=" + reservationId, e);
            throw e;
        }
    }

    public void saveCashPayment(Long reservationId,
                                Long agentId,
                                PaymentType type,
                                BigDecimal amount,
                                String notes) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();

            Payment existingPayment = session.createQuery(
                            "SELECT p FROM Payment p " +
                                    "WHERE p.reservation.id = :reservationId " +
                                    "AND p.type = :type",
                            Payment.class)
                    .setParameter("reservationId", reservationId)
                    .setParameter("type", type)
                    .uniqueResult();
            if (existingPayment != null) {
                throw new IllegalStateException("Un paiement de type " + type + " existe deja pour cette reservation.");
            }

            Reservation reservation = session.get(Reservation.class, reservationId);
            if (reservation == null) {
                throw new IllegalStateException("La reservation ciblee est introuvable.");
            }

            Payment payment = new Payment();
            payment.setReservation(reservation);
            if (agentId != null) {
                payment.setRecordedByAgent(session.get(com.bricolirent.domain.entity.Agent.class, agentId));
            }
            payment.setType(type);
            payment.setMethod(PaymentMethod.CASH);
            payment.setAmount(amount);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentDate(Instant.now());
            payment.setNotes(notes);
            session.createNativeMutationQuery(
                            "INSERT INTO payments " +
                                    "(reservation_id, recorded_by_agent_id, type, method, amount, status, payment_date, notes) " +
                                    "VALUES (:reservationId, :agentId, CAST(:type AS payment_type), CAST(:method AS payment_method), :amount, CAST(:status AS payment_status), :paymentDate, :notes)")
                    .setParameter("reservationId", reservationId)
                    .setParameter("agentId", agentId)
                    .setParameter("type", payment.getType().name())
                    .setParameter("method", payment.getMethod().name())
                    .setParameter("amount", payment.getAmount())
                    .setParameter("status", payment.getStatus().name())
                    .setParameter("paymentDate", payment.getPaymentDate())
                    .setParameter("notes", payment.getNotes())
                    .executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement d'un paiement cash pour reservation ID=" + reservationId, e);
            throw e;
        }
    }

    public void saveBeforeCheckoutPayments(Long reservationId,
                                           Long agentId,
                                           BigDecimal rentalAmount,
                                           BigDecimal depositAmount,
                                           String receiptNumber,
                                           String rentalNotes,
                                           String depositNotes) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();

            assertNoPaymentExists(session, reservationId, PaymentType.RENTAL);
            assertNoPaymentExists(session, reservationId, PaymentType.DEPOSIT);

            Reservation reservation = session.get(Reservation.class, reservationId);
            if (reservation == null) {
                throw new IllegalStateException("La reservation ciblee est introuvable.");
            }

            Instant paymentDate = Instant.now();
            insertPayment(session, reservationId, agentId, PaymentType.RENTAL, rentalAmount, receiptNumber, paymentDate, rentalNotes);
            insertPayment(session, reservationId, agentId, PaymentType.DEPOSIT, depositAmount, receiptNumber, paymentDate, depositNotes);

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de l'encaissement avant check-out pour reservation ID=" + reservationId, e);
            throw e;
        }
    }

    public List<Payment> findByRecordedByAgent(Long agentId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Payment> payments = session
                    .createQuery(
                            "SELECT p FROM Payment p " +
                                    "JOIN FETCH p.reservation r " +
                                    "JOIN FETCH r.tool " +
                                    "JOIN FETCH r.client c " +
                                    "JOIN FETCH c.users " +
                                    "WHERE p.recordedByAgent.id = :agentId " +
                                    "ORDER BY p.paymentDate DESC",
                            Payment.class)
                    .setParameter("agentId", agentId)
                    .getResultList();
            transaction.commit();
            return payments;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de l'historique des paiements pour agent ID=" + agentId, e);
            throw e;
        }
    }

    private void assertNoPaymentExists(Session session, Long reservationId, PaymentType type) {
        Payment existingPayment = session.createQuery(
                        "SELECT p FROM Payment p WHERE p.reservation.id = :reservationId AND p.type = :type",
                        Payment.class)
                .setParameter("reservationId", reservationId)
                .setParameter("type", type)
                .uniqueResult();
        if (existingPayment != null) {
            throw new IllegalStateException("Un paiement de type " + type + " existe deja pour cette reservation.");
        }
    }

    private void insertPayment(Session session,
                               Long reservationId,
                               Long agentId,
                               PaymentType type,
                               BigDecimal amount,
                               String receiptNumber,
                               Instant paymentDate,
                               String notes) {
        session.createNativeMutationQuery(
                        "INSERT INTO payments " +
                                "(reservation_id, recorded_by_agent_id, type, method, amount, status, payment_date, receipt_number, notes) " +
                                "VALUES (:reservationId, :agentId, CAST(:type AS payment_type), CAST(:method AS payment_method), :amount, CAST(:status AS payment_status), :paymentDate, :receiptNumber, :notes)")
                .setParameter("reservationId", reservationId)
                .setParameter("agentId", agentId)
                .setParameter("type", type.name())
                .setParameter("method", PaymentMethod.CASH.name())
                .setParameter("amount", amount)
                .setParameter("status", PaymentStatus.PAID.name())
                .setParameter("paymentDate", paymentDate)
                .setParameter("receiptNumber", receiptNumber)
                .setParameter("notes", notes)
                .executeUpdate();
    }
}
