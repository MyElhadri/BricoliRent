package com.bricolirent.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Utility class for Hibernate SessionFactory management.
 * Initializes a single SessionFactory instance from hibernate.cfg.xml.
 */
public class HibernateUtil {

    private static final SessionFactory sessionFactory;

    static {
        try {
            sessionFactory = new Configuration()
                    .configure("hibernate.cfg.xml")
                    .buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("SessionFactory initialization failed: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Returns the singleton SessionFactory instance.
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Closes the SessionFactory and releases all resources.
     */
    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}
