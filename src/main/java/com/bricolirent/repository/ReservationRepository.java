package com.bricolirent.repository;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.Tool;
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

    public List<Reservation> findApprovedWithToolAndClient() {
        return findByStatusWithToolAndClient(ReservationStatus.APPROVED);
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

    public void performCheckout(Long reservationId, Long agentId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();

            Reservation reservation = session.createQuery(
                            "SELECT r FROM Reservation r " +
                                    "JOIN FETCH r.tool " +
                                    "WHERE r.id = :reservationId",
                            Reservation.class)
                    .setParameter("reservationId", reservationId)
                    .uniqueResult();
            if (reservation == null) {
                throw new IllegalStateException("La reservation ciblee est introuvable.");
            }
            if (reservation.getStatus() != ReservationStatus.APPROVED) {
                throw new IllegalStateException("Seules les reservations approuvees peuvent etre servies en check-out.");
            }

            Tool tool = reservation.getTool();
            if (tool == null) {
                throw new IllegalStateException("L'outil associe a cette reservation est introuvable.");
            }

            Integer quantiteDisponible = tool.getAvailableQuantity();
            if (quantiteDisponible == null) {
                throw new IllegalStateException("La quantite disponible de l'outil est invalide.");
            }
            if (quantiteDisponible < reservation.getQuantity()) {
                throw new IllegalStateException("Stock insuffisant pour effectuer le check-out.");
            }

            tool.setAvailableQuantity(quantiteDisponible - reservation.getQuantity());

            reservation.setStatus(ReservationStatus.CHECKED_OUT);
            reservation.setCheckedOutAt(java.time.Instant.now());
            if (agentId != null) {
                reservation.setCheckoutAgent(session.get(com.bricolirent.domain.entity.Agent.class, agentId));
            }

            session.merge(tool);
            session.merge(reservation);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du check-out de la reservation ID=" + reservationId, e);
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

    public List<Reservation> findCheckoutHistoryByAgent(Long agentId) {
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
                                    "WHERE r.checkoutAgent.id = :agentId " +
                                    "AND r.checkedOutAt IS NOT NULL " +
                                    "AND r.status IN (:statuses) " +
                                    "ORDER BY r.checkedOutAt DESC",
                            Reservation.class)
                    .setParameter("agentId", agentId)
                    .setParameterList("statuses", List.of(
                            ReservationStatus.CHECKED_OUT,
                            ReservationStatus.RETURNED))
                    .getResultList();
            transaction.commit();
            return reservations;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de l'historique des check-outs pour agent ID=" + agentId, e);
            throw e;
        }
    }

    public List<Reservation> findAllDetailedForAdmin() {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Reservation> reservations = session
                    .createQuery(
                            "SELECT DISTINCT r FROM Reservation r " +
                                    "JOIN FETCH r.tool t " +
                                    "JOIN FETCH t.category " +
                                    "JOIN FETCH r.client c " +
                                    "JOIN FETCH c.users " +
                                    "LEFT JOIN FETCH r.handledByAgent ha " +
                                    "LEFT JOIN FETCH ha.users " +
                                    "LEFT JOIN FETCH r.checkoutAgent ca " +
                                    "LEFT JOIN FETCH ca.users " +
                                    "ORDER BY r.reservationDate DESC",
                            Reservation.class)
                    .getResultList();
            transaction.commit();
            return reservations;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement global des reservations admin", e);
            throw e;
        }
    }

    public long countByStatus(ReservationStatus status) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            Long count = session.createQuery(
                            "SELECT COUNT(r) FROM Reservation r WHERE r.status = :status",
                            Long.class)
                    .setParameter("status", status)
                    .getSingleResult();
            transaction.commit();
            return count == null ? 0L : count;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du comptage des reservations par statut : " + status, e);
            throw e;
        }
    }
}
