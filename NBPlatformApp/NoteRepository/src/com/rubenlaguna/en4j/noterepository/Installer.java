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
    private static EntityManager EM = null;
    private static String connectionURL = null;
    private static final Logger LOG = Logger.getLogger(Installer.class.getName());

    @Override
    public void close() {
        if (null != EM) {
            synchronized (EM) {
                LOG.info("JPA Native Query shutdown");
                EM.getTransaction().begin();
                EM.createNativeQuery("SHUTDOWN").executeUpdate();
                EM.getTransaction().commit();

                LOG.info("closing EntityManager " + EM);
                EM.close();
            }
        }
        if (null != EMF) {
            LOG.info("closing EntityManagerFactory " + EMF);
            EMF.close();
        }

    }

    public static EntityManager getEntityManager() {
        if (EMF == null) {
            Map properties = new HashMap();
            connectionURL = "jdbc:hsqldb:file:" + System.getProperty("netbeans.user") + "/en4j/db";
            properties.put("openjpa.ConnectionURL", connectionURL);
            EMF = javax.persistence.Persistence.createEntityManagerFactory("JpaEntitiesClassLibraryPU", properties);
        }

        if (null == EM) {
            EM = EMF.createEntityManager();
        }
        return java.beans.Beans.isDesignTime() ? null : EM;
    }
}
