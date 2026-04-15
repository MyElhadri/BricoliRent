package com.bricolirent.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for Hibernate SessionFactory management.
 * Initializes a single SessionFactory instance from hibernate.cfg.xml.
 *
 * Les paramètres sensibles (connection.url, connection.username, connection.password)
 * sont chargés depuis db-local.properties (non versionné, à créer depuis
 * db-local.example.properties) et injectés avant la création de la SessionFactory.
 */
public class HibernateUtil {

    private static final SessionFactory sessionFactory;

    static {
        try {
            // Charge les paramètres sensibles depuis db-local.properties (classpath)
            Properties dbLocalProps = new Properties();
            try (InputStream is = HibernateUtil.class
                    .getClassLoader()
                    .getResourceAsStream("db-local.properties")) {
                if (is == null) {
                    throw new IOException(
                        "db-local.properties introuvable dans le classpath. " +
                        "Copiez db-local.example.properties en db-local.properties " +
                        "et renseignez vos paramètres de connexion.");
                }
                dbLocalProps.load(is);
            }

            // Construit la SessionFactory en fusionnant hibernate.cfg.xml
            // avec les propriétés locales sensibles
            sessionFactory = new Configuration()
                    .configure("hibernate.cfg.xml")
                    .addProperties(dbLocalProps)
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
