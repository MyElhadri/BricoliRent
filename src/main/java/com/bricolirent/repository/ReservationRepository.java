package com.bricolirent.repository;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.enums.ReservationStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository pour l'entité Reservation.
 * Fournit les opérations CRUD héritées + recherches par client, statut et réservations actives.
 */
public class ReservationRepository extends GenericRepository<Reservation, Long> {

    private static final Logger LOGGER = Logger.getLogger(ReservationRepository.class.getName());

    public ReservationRepository() {
        super(Reservation.class);
    }

    /**
     * Récupère toutes les réservations d'un client donné.
     *
     * @param clientId L'identifiant du client
     * @return La liste des réservations du client
     */
    public List<Reservation> findByClientId(Long clientId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Reservation> reservations = session
                    .createQuery(
                            "FROM Reservation r WHERE r.client.id = :clientId ORDER BY r.reservationDate DESC",
                            Reservation.class)
                    .setParameter("clientId", clientId)
                    .getResultList();
            transaction.commit();
            return reservations;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche des réservations du client ID=" + clientId, e);
            throw e;
        }
    }

    /**
     * Récupère les réservations actives (statut PENDING, APPROVED ou CHECKED_OUT).
     * Ce sont les réservations qui ne sont ni retournées, ni rejetées.
     *
     * @return La liste des réservations actives
     */
    public List<Reservation> findActiveReservations() {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Reservation> reservations = session
                    .createQuery(
                            "FROM Reservation r WHERE r.status IN (:statuses) ORDER BY r.reservationDate DESC",
                            Reservation.class)
                    .setParameterList("statuses", List.of(
                            ReservationStatus.PENDING,
                            ReservationStatus.APPROVED,
                            ReservationStatus.CHECKED_OUT))
                    .getResultList();
            transaction.commit();
            return reservations;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche des réservations actives", e);
            throw e;
        }
    }

    /**
     * Recherche les réservations par statut.
     *
     * @param status Le statut de réservation à filtrer
     * @return La liste des réservations ayant ce statut
     */
    public List<Reservation> findByStatus(ReservationStatus status) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Reservation> reservations = session
                    .createQuery(
                            "FROM Reservation r WHERE r.status = :status ORDER BY r.reservationDate DESC",
                            Reservation.class)
                    .setParameter("status", status)
                    .getResultList();
            transaction.commit();
            return reservations;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche par statut : " + status, e);
            throw e;
        }
    }
}
