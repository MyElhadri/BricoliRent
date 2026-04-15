package com.bricolirent.repository;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.enums.ReservationStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReservationRepository extends GenericRepository<Reservation, Long> {

    private static final Logger LOGGER = Logger.getLogger(ReservationRepository.class.getName());

    public ReservationRepository() {
        super(Reservation.class);
    }

    public void saveForClientAndTool(Long clientId, Long toolId, Reservation reservation) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();

            // Utilisation de session.get() au lieu decsession.getReference() 
            // pour contourner le typage des Proxies Hibernate sur @MapsId (Client)
            com.bricolirent.domain.entity.Client client = session.get(com.bricolirent.domain.entity.Client.class, clientId);
            com.bricolirent.domain.entity.Tool tool = session.get(com.bricolirent.domain.entity.Tool.class, toolId);

            reservation.setClient(client);
            reservation.setTool(tool);

            session.persist(reservation);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la creation transactionnelle de la reservation", e);
            throw e;
        }
    }

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
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche des reservations du client ID=" + clientId, e);
            throw e;
        }
    }

    public List<Reservation> findByClientIdWithTool(Long clientId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Reservation> reservations = session
                    .createQuery(
                            "SELECT r FROM Reservation r JOIN FETCH r.tool WHERE r.client.id = :clientId ORDER BY r.reservationDate DESC",
                            Reservation.class)
                    .setParameter("clientId", clientId)
                    .getResultList();
            transaction.commit();
            return reservations;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement detaille des reservations du client ID=" + clientId, e);
            throw e;
        }
    }

    public Optional<Reservation> findByIdAndClientId(Long reservationId, Long clientId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            Reservation reservation = session
                    .createQuery(
                            "SELECT r FROM Reservation r JOIN FETCH r.tool WHERE r.id = :reservationId AND r.client.id = :clientId",
                            Reservation.class)
                    .setParameter("reservationId", reservationId)
                    .setParameter("clientId", clientId)
                    .uniqueResult();
            transaction.commit();
            return Optional.ofNullable(reservation);
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de la reservation ID=" + reservationId + " pour client ID=" + clientId, e);
            throw e;
        }
    }

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
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche des reservations actives", e);
            throw e;
        }
    }

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

    public List<Reservation> findByStatusWithToolAndClient(ReservationStatus status) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Reservation> reservations = session
                    .createQuery(
                            "SELECT r FROM Reservation r " +
                                    "JOIN FETCH r.tool " +
                                    "JOIN FETCH r.client c " +
                                    "JOIN FETCH c.users " +
                                    "WHERE r.status = :status " +
                                    "ORDER BY r.reservationDate ASC",
                            Reservation.class)
                    .setParameter("status", status)
                    .getResultList();
            transaction.commit();
            return reservations;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement detaille des reservations par statut : " + status, e);
            throw e;
        }
    }

    public long countActiveReservationsForClient(Long clientId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            Long count = session.createQuery(
                            "SELECT COUNT(r) FROM Reservation r " +
                                    "WHERE r.client.id = :clientId " +
                                    "AND r.status IN (:statuses)",
                            Long.class)
                    .setParameter("clientId", clientId)
                    .setParameterList("statuses", List.of(
                            ReservationStatus.PENDING,
                            ReservationStatus.APPROVED,
                            ReservationStatus.CHECKED_OUT))
                    .getSingleResult();
            transaction.commit();
            return count == null ? 0 : count;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du comptage des reservations actives du client ID=" + clientId, e);
            throw e;
        }
    }

    public Optional<Reservation> findByIdWithToolAndClient(Long reservationId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            Reservation reservation = session.createQuery(
                            "SELECT r FROM Reservation r " +
                                    "JOIN FETCH r.tool " +
                                    "JOIN FETCH r.client c " +
                                    "JOIN FETCH c.users " +
                                    "WHERE r.id = :reservationId",
                            Reservation.class)
                    .setParameter("reservationId", reservationId)
                    .uniqueResult();
            transaction.commit();
            return Optional.ofNullable(reservation);
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement detaille de la reservation ID=" + reservationId, e);
            throw e;
        }
    }

    public void updateDecision(Long reservationId, Long agentId, ReservationStatus status, String reason, boolean automatic) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();

            Reservation reservation = session.get(Reservation.class, reservationId);
            if (reservation == null) {
                throw new IllegalStateException("La reservation ciblee est introuvable.");
            }

            reservation.setStatus(status);
            reservation.setApprovalReason(reason);
            reservation.setApprovedAutomatically(automatic);
            reservation.setApprovedAt(java.time.Instant.now());
            if (agentId != null) {
                reservation.setHandledByAgent(session.get(com.bricolirent.domain.entity.Agent.class, agentId));
            }

            session.merge(reservation);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise a jour de decision pour reservation ID=" + reservationId, e);
            throw e;
        }
    }

    public List<Reservation> findHandledByAgent(Long agentId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Reservation> reservations = session
                    .createQuery(
                            "SELECT r FROM Reservation r " +
                                    "JOIN FETCH r.tool " +
                                    "JOIN FETCH r.client c " +
                                    "JOIN FETCH c.users " +
                                    "WHERE r.handledByAgent.id = :agentId " +
                                    "AND r.status IN (:statuses) " +
                                    "ORDER BY r.approvedAt DESC, r.reservationDate DESC",
                            Reservation.class)
                    .setParameter("agentId", agentId)
                    .setParameterList("statuses", List.of(
                            ReservationStatus.APPROVED,
                            ReservationStatus.REJECTED))
                    .getResultList();
            transaction.commit();
            return reservations;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de l'historique agent ID=" + agentId, e);
            throw e;
        }
    }
}
