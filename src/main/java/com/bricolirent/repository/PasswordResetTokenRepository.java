package com.bricolirent.repository;

import com.bricolirent.domain.entity.PasswordResetToken;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.Optional;
import com.bricolirent.domain.entity.User;

public class PasswordResetTokenRepository extends GenericRepository<PasswordResetToken, Long> {

    public PasswordResetTokenRepository() {
        super(PasswordResetToken.class);
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        try {
            Session session = getCurrentSession();
            // Start transaction might be needed purely to avoid session closed errors or use open ones. 
            // GenericRepository methods generally open and commit a transaction. We will do read-only transaction natively.
            session.beginTransaction();
            Query<PasswordResetToken> query = session.createQuery("FROM PasswordResetToken t WHERE t.token = :token", PasswordResetToken.class);
            query.setParameter("token", token);
            PasswordResetToken result = query.uniqueResult();
            session.getTransaction().commit();
            return Optional.ofNullable(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<PasswordResetToken> findByUser(User user) {
        try {
            Session session = getCurrentSession();
            session.beginTransaction();
            Query<PasswordResetToken> query = session.createQuery("FROM PasswordResetToken t WHERE t.user = :user", PasswordResetToken.class);
            query.setParameter("user", user);
            PasswordResetToken result = query.uniqueResult();
            session.getTransaction().commit();
            return Optional.ofNullable(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
