package com.bricolirent.repository;

import com.bricolirent.domain.entity.ReturnRecord;
import com.bricolirent.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ReturnRecord entity operations.
 */
public class ReturnRecordRepository {

    public Optional<ReturnRecord> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(ReturnRecord.class, id));
        }
    }

    public List<ReturnRecord> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ReturnRecord", ReturnRecord.class).list();
        }
    }

    public Optional<ReturnRecord> findByReservationId(Long reservationId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM ReturnRecord WHERE reservation.id = :resId", ReturnRecord.class)
                    .setParameter("resId", reservationId)
                    .uniqueResultOptional();
        }
    }

    public void save(ReturnRecord returnRecord) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(returnRecord);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void update(ReturnRecord returnRecord) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(returnRecord);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void delete(ReturnRecord returnRecord) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.remove(returnRecord);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
