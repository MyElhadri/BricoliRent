package com.bricolirent.repository;

import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Reservation entity operations.
 */
public class ReservationRepository {

    public Optional<Reservation> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Reservation.class, id));
        }
    }

    public List<Reservation> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Reservation", Reservation.class).list();
        }
    }

    public List<Reservation> findByClientId(Long clientId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Reservation WHERE client.id = :clientId", Reservation.class)
                    .setParameter("clientId", clientId)
                    .list();
        }
    }

    public List<Reservation> findByStatus(ReservationStatus status) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Reservation WHERE status = :status", Reservation.class)
                    .setParameter("status", status)
                    .list();
        }
    }

    public void save(Reservation reservation) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(reservation);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void update(Reservation reservation) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(reservation);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void delete(Reservation reservation) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.remove(reservation);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
