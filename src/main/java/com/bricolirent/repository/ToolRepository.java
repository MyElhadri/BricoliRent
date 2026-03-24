package com.bricolirent.repository;

import com.bricolirent.domain.entity.Tool;
import com.bricolirent.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Tool entity operations.
 */
public class ToolRepository {

    public Optional<Tool> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Tool.class, id));
        }
    }

    public List<Tool> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Tool", Tool.class).list();
        }
    }

    public List<Tool> findByCategory(Long categoryId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Tool WHERE category.id = :catId", Tool.class)
                    .setParameter("catId", categoryId)
                    .list();
        }
    }

    public List<Tool> findAvailable() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Tool WHERE available = true AND availableQuantity > 0", Tool.class)
                    .list();
        }
    }

    public void save(Tool tool) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(tool);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void update(Tool tool) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(tool);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void delete(Tool tool) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.remove(tool);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
