/*
 *  Copyright (C) 2010 Ruben Laguna <ruben.laguna@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.rubenlaguna.en4j.noterepository;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.openide.modules.ModuleInstall;

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
