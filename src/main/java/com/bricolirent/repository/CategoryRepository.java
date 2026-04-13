package com.bricolirent.repository;

import com.bricolirent.domain.entity.Category;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository pour l'entité Category.
 * Fournit les opérations CRUD héritées + recherche par nom.
 */
public class CategoryRepository extends GenericRepository<Category, Long> {

    private static final Logger LOGGER = Logger.getLogger(CategoryRepository.class.getName());

    public CategoryRepository() {
        super(Category.class);
    }

    /**
     * Recherche une catégorie par son nom exact.
     * Le nom de catégorie est supposé unique en base.
     *
     * @param name Le nom de la catégorie
     * @return Un Optional contenant la catégorie si trouvée
     */
    public Optional<Category> findByName(String name) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            Category category = session
                    .createQuery("FROM Category c WHERE c.name = :name", Category.class)
                    .setParameter("name", name)
                    .uniqueResult();
            transaction.commit();
            return Optional.ofNullable(category);
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche de la catégorie : " + name, e);
            throw e;
        }
    }
}
