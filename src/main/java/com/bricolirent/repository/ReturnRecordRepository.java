package com.bricolirent.repository;

import com.bricolirent.domain.entity.ReturnRecord;
import org.hibernate.Session;
import org.hibernate.Transaction;

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
}
