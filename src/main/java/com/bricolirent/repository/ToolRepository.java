package com.bricolirent.repository;

import com.bricolirent.domain.entity.Tool;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository pour l'entité Tool.
 * Fournit les opérations CRUD héritées + recherches par disponibilité, catégorie et nom.
 */
public class ToolRepository extends GenericRepository<Tool, Long> {

    private static final Logger LOGGER = Logger.getLogger(ToolRepository.class.getName());

    public ToolRepository() {
        super(Tool.class);
    }

    /**
     * Surcharge de findAll pour inclure la catégorie (JOIN FETCH) et éviter l'erreur No Session (LazyInitializationException)
     */
    @Override
    public List<Tool> findAll() {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Tool> tools = session
                    .createQuery("FROM Tool t JOIN FETCH t.category", Tool.class)
                    .getResultList();
            transaction.commit();
            return tools;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur findAll (avec fetch) sur Tool", e);
            throw e;
        }
    }

    /**
     * Récupère les outils disponibles (quantité disponible > 0 et actif).
     *
     * @return La liste des outils actuellement disponibles à la location
     */
    public List<Tool> findAvailableTools() {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Tool> tools = session
                    .createQuery(
                            "FROM Tool t WHERE t.availableQuantity > 0 AND t.active = true",
                            Tool.class)
                    .getResultList();
            transaction.commit();
            return tools;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche des outils disponibles", e);
            throw e;
        }
    }

    /**
     * Recherche les outils par nom de catégorie.
     *
     * @param category Le nom de la catégorie
     * @return La liste des outils appartenant à cette catégorie
     */
    public List<Tool> findByCategory(String category) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Tool> tools = session
                    .createQuery(
                            "FROM Tool t WHERE t.category.name = :category",
                            Tool.class)
                    .setParameter("category", category)
                    .getResultList();
            transaction.commit();
            return tools;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche par catégorie : " + category, e);
            throw e;
        }
    }

    /**
     * Recherche les outils dont le nom contient un mot-clé (insensible à la casse).
     *
     * @param keyword Le mot-clé de recherche
     * @return La liste des outils correspondants
     */
    public List<Tool> findByNameContaining(String keyword) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            List<Tool> tools = session
                    .createQuery(
                            "FROM Tool t WHERE LOWER(t.name) LIKE LOWER(:keyword)",
                            Tool.class)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();
            transaction.commit();
            return tools;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche par mot-clé : " + keyword, e);
            throw e;
        }
    }

    /**
     * Vérifie si un outil existe déjà avec le même nom dans la même catégorie.
     * En mode édition, exclut l'ID de l'outil en cours.
     */
    public boolean existsByNameAndCategory(String name, Long categoryId, Long excludeToolId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();

            String hql = "SELECT count(t) FROM Tool t WHERE LOWER(t.name) = LOWER(:name) AND t.category.id = :categoryId";
            if (excludeToolId != null) {
                hql += " AND t.id != :excludeToolId";
            }

            org.hibernate.query.Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("name", name);
            query.setParameter("categoryId", categoryId);

            if (excludeToolId != null) {
                query.setParameter("excludeToolId", excludeToolId);
            }

            Long count = query.getSingleResult();
            transaction.commit();
            return count > 0;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification d'unicité outil", e);
            throw e;
        }
    }
}
