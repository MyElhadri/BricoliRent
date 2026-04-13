package com.bricolirent.repository;

import com.bricolirent.domain.entity.Payment;
import org.hibernate.Session;
import org.hibernate.Transaction;

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
}
