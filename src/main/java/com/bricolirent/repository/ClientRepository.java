package com.bricolirent.repository;

import com.bricolirent.domain.entity.Client;
import com.bricolirent.domain.entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository pour l'entité Client.
 */
public class ClientRepository extends GenericRepository<Client, Long> {
    
    private static final Logger LOGGER = Logger.getLogger(ClientRepository.class.getName());

    public ClientRepository() {
        super(Client.class);
    }

    public Optional<Client> findByIdWithUser(Long clientId) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            Client client = session.createQuery(
                            "SELECT c FROM Client c JOIN FETCH c.users WHERE c.id = :clientId",
                            Client.class)
                    .setParameter("clientId", clientId)
                    .uniqueResult();
            transaction.commit();
            return Optional.ofNullable(client);
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception rbEx) {
                    LOGGER.log(Level.SEVERE, "Echec du rollback", rbEx);
                }
            }
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement du client avec utilisateur (ID=" + clientId + ")", e);
            throw e;
        }
    }
    
    /**
     * Sauvegarde simultanément un User et son Client rattaché dans la même transaction.
     * Cela empêche les erreurs d'entités détachées car le User reste "managed" (attaché)
     * au moment où le Client est persisté avec la même session.
     */
    public void saveUserAndClient(User user, Client client) {
        Transaction transaction = null;
        try {
            Session session = getCurrentSession();
            transaction = session.beginTransaction();
            
            // On persiste l'utilisateur d'abord (il devient managed et reçoit son ID)
            session.persist(user);
            
            // On attache l'utilisateur (actuellement managed) au client
            client.setUsers(user);
            
            // On persiste le client (sans erreur car user n'est pas detached)
            session.persist(client);
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch(Exception rbEx) {
                    LOGGER.log(Level.SEVERE, "Échec du rollback", rbEx);
                }
            }
            LOGGER.log(Level.SEVERE, "Erreur transactionnelle lors de la création User + Client", e);
            throw new RuntimeException("Échec de la transaction d'inscription", e);
        }
    }
}
