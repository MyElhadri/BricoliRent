package com.bricolirent.repository;

import com.bricolirent.domain.entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository pour l'entité User.
 * Fournit les opérations CRUD héritées + recherche par email.
 */
public class UserRepository extends GenericRepository<User, Long> {

    private static final Logger LOGGER = Logger.getLogger(UserRepository.class.getName());

    public UserRepository() {
        super(User.class);
    }

    /**
     * Recherche un utilisateur par son adresse email.
     * L'email est unique en base, donc on attend au plus un résultat.
     *
     * @param email L'adresse email à rechercher
     * @return Un Optional contenant l'utilisateur si trouvé
     */
    public Optional<User> findByEmail(String email) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            User user = session
                    .createQuery("FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .uniqueResult();
            transaction.commit();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche par email : " + email, e);
            throw e;
        }
    }
}
