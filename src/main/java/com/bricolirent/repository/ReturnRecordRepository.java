package com.bricolirent.repository;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.ReturnRecord;
import com.bricolirent.domain.entity.Tool;
import com.bricolirent.domain.enums.ReservationStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository pour l'entité ReturnRecord.
 * Fournit les opérations CRUD héritées + recherche des retours en retard.
 */
public class ReturnRecordRepository extends GenericRepository<ReturnRecord, Long> {

    private static final Logger LOGGER = Logger.getLogger(ReturnRecordRepository.class.getName());

    public ReturnRecordRepository() {
        super(ReturnRecord.class);
    }

    /**
     * Récupère les retours en retard (nombre de jours de retard > 0).
     * Utile pour le suivi des pénalités et les tableaux de bord.
     *
     * @return La liste des retours avec des jours de retard
     */
    public List<ReturnRecord> findLateReturns() {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<ReturnRecord> lateReturns = session
                    .createQuery(
                            "FROM ReturnRecord rr WHERE rr.lateDays > 0 ORDER BY rr.lateDays DESC",
                            ReturnRecord.class)
                    .getResultList();
            transaction.commit();
            return lateReturns;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche des retours en retard", e);
            throw e;
        }
    }

    public ReturnRecord registerReturn(Long reservationId,
                                       Long agentId,
                                       Instant actualReturnDate,
                                       int lateDays,
                                       BigDecimal latePenalty,
                                       String notes) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();

            ReturnRecord existingRecord = session.createQuery(
                            "SELECT rr FROM ReturnRecord rr WHERE rr.reservation.id = :reservationId",
                            ReturnRecord.class)
                    .setParameter("reservationId", reservationId)
                    .uniqueResult();
            if (existingRecord != null) {
                Reservation linkedReservation = existingRecord.getReservation();
                if (linkedReservation != null && linkedReservation.getStatus() != ReservationStatus.RETURNED) {
                    Tool linkedTool = linkedReservation.getTool();
                    if (linkedTool != null) {
                        Integer currentAvailable = linkedTool.getAvailableQuantity();
                        linkedTool.setAvailableQuantity((currentAvailable == null ? 0 : currentAvailable) + linkedReservation.getQuantity());
                        session.merge(linkedTool);
                    }
                    linkedReservation.setStatus(ReservationStatus.RETURNED);
                    session.merge(linkedReservation);
                }
                transaction.commit();
                return existingRecord;
            }

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
            if (reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
                throw new IllegalStateException("Seules les reservations en check-out peuvent etre retournees.");
            }

            Tool tool = reservation.getTool();
            if (tool == null) {
                throw new IllegalStateException("L'outil associe a cette reservation est introuvable.");
            }

            Integer quantiteDisponible = tool.getAvailableQuantity();
            tool.setAvailableQuantity((quantiteDisponible == null ? 0 : quantiteDisponible) + reservation.getQuantity());
            reservation.setStatus(ReservationStatus.RETURNED);

            ReturnRecord record = new ReturnRecord();
            record.setReservation(reservation);
            record.setActualReturnDate(actualReturnDate);
            record.setLateDays(lateDays);
            record.setLatePenalty(latePenalty);
            record.setNotes(notes);
            if (agentId != null) {
                record.setHandledByAgent(session.get(com.bricolirent.domain.entity.Agent.class, agentId));
            }

            session.merge(tool);
            session.merge(reservation);
            session.persist(record);
            transaction.commit();
            return record;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement du retour pour reservation ID=" + reservationId, e);
            throw e;
        }
    }

    public List<ReturnRecord> findByHandledByAgent(Long agentId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<ReturnRecord> records = session.createQuery(
                            "SELECT rr FROM ReturnRecord rr " +
                                    "JOIN FETCH rr.reservation r " +
                                    "JOIN FETCH r.tool " +
                                    "JOIN FETCH r.client c " +
                                    "JOIN FETCH c.users " +
                                    "WHERE rr.handledByAgent.id = :agentId " +
                                    "ORDER BY rr.actualReturnDate DESC",
                            ReturnRecord.class)
                    .setParameter("agentId", agentId)
                    .getResultList();
            transaction.commit();
            return records;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de l'historique des retours pour agent ID=" + agentId, e);
            throw e;
        }
    }
}
