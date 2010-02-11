/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    private static EntityManagerFactory EMF = null;
    private static String connectionURL = null;
    private static final Logger LOG = Logger.getLogger(Installer.class.getName());

    @Override
    public void installed() {
        EntityManager entityManager = getEntityManagerFactory().createEntityManager();
        LOG.info("setting properties in HSQLDB");
        entityManager.getTransaction().begin();
        entityManager.createNativeQuery("SET FILES LOG SIZE 40").executeUpdate();
        //entityManager.createNativeQuery("SET FILES CACHE SIZE 5000").executeUpdate();
        entityManager.getTransaction().commit();
        entityManager.close();

    }

    @Override
    public void close() {
        if (null != EMF) {
            LOG.info("JPA Native Query shutdown");
            EntityManager entityManager = EMF.createEntityManager();
            entityManager.getTransaction().begin();
            entityManager.createNativeQuery("SHUTDOWN").executeUpdate();
            entityManager.getTransaction().commit();
            entityManager.close();
            LOG.info("closing EntityManagerFactory " + EMF);
            EMF.close();
        }

    }

    public static EntityManagerFactory getEntityManagerFactory() {
        if (EMF == null) {
            Map properties = new HashMap();
            connectionURL = "jdbc:hsqldb:file:" + System.getProperty("netbeans.user") + "/en4j/db";
            properties.put("openjpa.ConnectionURL", connectionURL);
            EMF = javax.persistence.Persistence.createEntityManagerFactory("JpaEntitiesClassLibraryPU", properties);
        }
        return EMF;

    }
}
