package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Agent;
import com.bricolirent.domain.entity.User;
import com.bricolirent.repository.UserRepository;
import com.bricolirent.security.PasswordHashUtil;
import com.bricolirent.service.AdminUserService;
import com.bricolirent.util.HibernateUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class AdminUserServiceImpl implements AdminUserService {

    private UserRepository userRepository;

    @PostConstruct
    public void init() {
        this.userRepository = new UserRepository();
    }

    @Override
    public void createAgent(String fullName, String email, String rawPassword, String employeeCode) {
        String normalizedFullName = normalizeRequired(fullName, "Le nom complet est obligatoire.");
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = normalizeRequired(rawPassword, "Le mot de passe initial est obligatoire.");
        String normalizedEmployeeCode = normalizeRequired(employeeCode, "Le code employe est obligatoire.").toUpperCase(Locale.ROOT);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalStateException("Cette adresse e-mail est deja utilisee.");
        }

        Transaction transaction = null;
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            transaction = session.beginTransaction();

            Long count = session.createQuery(
                            "SELECT count(a) FROM Agent a WHERE UPPER(a.employeeCode) = :employeeCode",
                            Long.class)
                    .setParameter("employeeCode", normalizedEmployeeCode)
                    .getSingleResult();

            if (count != null && count > 0) {
                throw new IllegalStateException("Ce code employe existe deja.");
            }

            User user = new User();
            user.setFullName(normalizedFullName);
            user.setEmail(normalizedEmail);
            user.setPasswordHash(PasswordHashUtil.hash(normalizedPassword));
            user.setActive(true);

            session.persist(user);

            Agent agent = new Agent();
            agent.setUsers(user);
            agent.setEmployeeCode(normalizedEmployeeCode);

            session.persist(agent);
            transaction.commit();
        } catch (IllegalStateException e) {
            rollbackQuietly(transaction);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(transaction);
            throw new RuntimeException("Une erreur technique est survenue lors de la creation de l'agent.", e);
        }
    }

    @Override
    public void deleteAgent(Long agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("L'agent a supprimer est invalide.");
        }

        Transaction transaction = null;
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            transaction = session.beginTransaction();

            Agent agent = session.createQuery(
                            "SELECT a FROM Agent a JOIN FETCH a.users WHERE a.id = :id",
                            Agent.class)
                    .setParameter("id", agentId)
                    .uniqueResult();

            if (agent == null) {
                throw new IllegalStateException("L'agent selectionne est introuvable.");
            }

            User user = agent.getUsers();
            session.remove(agent);

            if (user != null) {
                session.remove(user);
            }

            transaction.commit();
        } catch (IllegalArgumentException | IllegalStateException e) {
            rollbackQuietly(transaction);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(transaction);
            throw new RuntimeException("Une erreur technique est survenue lors de la suppression de l'agent.", e);
        }
    }

    @Override
    public void toggleAgentActive(Long agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("L'agent selectionne est invalide.");
        }

        Transaction transaction = null;
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            transaction = session.beginTransaction();

            Agent agent = session.createQuery(
                            "SELECT a FROM Agent a JOIN FETCH a.users WHERE a.id = :id",
                            Agent.class)
                    .setParameter("id", agentId)
                    .uniqueResult();

            if (agent == null || agent.getUsers() == null) {
                throw new IllegalStateException("L'agent selectionne est introuvable.");
            }

            User user = agent.getUsers();
            user.setActive(!Boolean.TRUE.equals(user.getActive()));
            session.merge(user);

            transaction.commit();
        } catch (IllegalArgumentException | IllegalStateException e) {
            rollbackQuietly(transaction);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(transaction);
            throw new RuntimeException("Une erreur technique est survenue lors du changement de statut de l'agent.", e);
        }
    }

    @Override
    public List<Agent> getAllAgents() {
        Transaction transaction = null;
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            transaction = session.beginTransaction();

            List<Agent> agents = session.createQuery(
                            "SELECT a FROM Agent a JOIN FETCH a.users ORDER BY a.users.fullName",
                            Agent.class)
                    .getResultList();

            transaction.commit();
            return agents;
        } catch (Exception e) {
            rollbackQuietly(transaction);
            throw new RuntimeException("Impossible de charger la liste des agents.", e);
        }
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        String normalizedEmail = normalizeRequired(email, "L'adresse e-mail est obligatoire.").toLowerCase(Locale.ROOT);
        if (!normalizedEmail.contains("@")) {
            throw new IllegalArgumentException("L'adresse e-mail est invalide.");
        }
        return normalizedEmail;
    }

    private void rollbackQuietly(Transaction transaction) {
        if (transaction != null && transaction.isActive()) {
            try {
                transaction.rollback();
            } catch (Exception ignored) {
                // pas d'action supplementaire
            }
        }
    }
}
