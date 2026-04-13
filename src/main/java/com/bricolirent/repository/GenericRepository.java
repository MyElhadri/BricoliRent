package com.bricolirent.repository;

import com.bricolirent.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository générique fournissant les opérations CRUD de base.
 * Toutes les classes repository spécifiques héritent de cette classe.
 *
 * <p>Utilise Hibernate 6 natif avec SessionFactory et gestion manuelle des transactions.</p>
 *
 * @param <T>  Le type de l'entité
 * @param <ID> Le type de l'identifiant (doit être Serializable)
 */
public abstract class GenericRepository<T, ID extends Serializable> {

    private static final Logger LOGGER = Logger.getLogger(GenericRepository.class.getName());

    /** Le type de l'entité gérée, nécessaire pour les requêtes Hibernate */
    private final Class<T> entityClass;

    /** SessionFactory partagée, initialisée via HibernateUtil */
    protected final SessionFactory sessionFactory;

    /**
     * Constructeur protégé appelé par les sous-classes.
     *
     * @param entityClass La classe de l'entité gérée (ex: User.class)
     */
    protected GenericRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    /**
     * Retourne la session courante liée à la transaction en cours.
     * La session est gérée par le contexte transactionnel de Hibernate.
     */
    protected Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    // ========================================================================
    // OPÉRATIONS CRUD
    // ========================================================================

    /**
     * Persiste une nouvelle entité en base de données.
     *
     * @param entity L'entité à sauvegarder
     */
    public void save(T entity) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            session.persist(entity);
            transaction.commit();
        } catch (Exception e) {
            rollbackQuietly(transaction);
            LOGGER.log(Level.SEVERE, "Erreur lors de la sauvegarde de l'entité " + entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * Recherche une entité par son identifiant.
     *
     * @param id L'identifiant de l'entité
     * @return Un Optional contenant l'entité si trouvée, vide sinon
     */
    public Optional<T> findById(ID id) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            T entity = session.get(entityClass, id);
            transaction.commit();
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            rollbackQuietly(transaction);
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche par ID pour " + entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * Récupère toutes les entités de ce type depuis la base de données.
     *
     * @return La liste de toutes les entités
     */
    public List<T> findAll() {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<T> entities = session
                    .createQuery("FROM " + entityClass.getSimpleName(), entityClass)
                    .getResultList();
            transaction.commit();
            return entities;
        } catch (Exception e) {
            rollbackQuietly(transaction);
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération de tous les " + entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * Met à jour une entité existante en base de données.
     *
     * @param entity L'entité avec les modifications à persister
     */
    public void update(T entity) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            session.merge(entity);
            transaction.commit();
        } catch (Exception e) {
            rollbackQuietly(transaction);
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour de " + entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * Supprime une entité de la base de données.
     *
     * @param entity L'entité à supprimer
     */
    public void delete(T entity) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            // merge pour rattacher l'entité à la session si détachée
            T managedEntity = session.merge(entity);
            session.remove(managedEntity);
            transaction.commit();
        } catch (Exception e) {
            rollbackQuietly(transaction);
            LOGGER.log(Level.SEVERE, "Erreur lors de la suppression de " + entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * Supprime une entité par son identifiant.
     * Charge l'entité puis la supprime pour garantir les cascades.
     *
     * @param id L'identifiant de l'entité à supprimer
     */
    public void deleteById(ID id) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            T entity = session.get(entityClass, id);
            if (entity != null) {
                session.remove(entity);
            }
            transaction.commit();
        } catch (Exception e) {
            rollbackQuietly(transaction);
            LOGGER.log(Level.SEVERE, "Erreur lors de la suppression par ID pour " + entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * Compte le nombre total d'entités de ce type en base.
     *
     * @return Le nombre d'entités
     */
    public long count() {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            Long result = session
                    .createQuery("SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class)
                    .getSingleResult();
            transaction.commit();
            return result;
        } catch (Exception e) {
            rollbackQuietly(transaction);
            LOGGER.log(Level.SEVERE, "Erreur lors du comptage de " + entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * Vérifie l'existence d'une entité par son identifiant.
     *
     * @param id L'identifiant à vérifier
     * @return true si l'entité existe, false sinon
     */
    public boolean existsById(ID id) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            T entity = session.get(entityClass, id);
            transaction.commit();
            return entity != null;
        } catch (Exception e) {
            rollbackQuietly(transaction);
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification d'existence pour " + entityClass.getSimpleName(), e);
            throw e;
        }
    }

    // ========================================================================
    // UTILITAIRES INTERNES
    // ========================================================================

    /**
     * Effectue un rollback silencieux de la transaction.
     * Évite de masquer l'exception originale en cas d'échec du rollback.
     *
     * @param transaction La transaction à annuler (peut être null)
     */
    private void rollbackQuietly(Transaction transaction) {
        if (transaction != null && transaction.isActive()) {
            try {
                transaction.rollback();
            } catch (Exception rollbackEx) {
                LOGGER.log(Level.SEVERE, "Échec du rollback de la transaction", rollbackEx);
            }
        }
    }

    /**
     * Retourne la classe de l'entité gérée.
     * Utile pour les sous-classes qui ont besoin de construire des requêtes dynamiques.
     *
     * @return La classe de l'entité
     */
    protected Class<T> getEntityClass() {
        return entityClass;
    }
}
