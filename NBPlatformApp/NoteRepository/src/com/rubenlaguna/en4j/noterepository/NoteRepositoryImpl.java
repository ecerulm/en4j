/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.jpaentities.Notes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 *
 * @author ecerulm
 */
public class NoteRepositoryImpl implements NoteRepository {

    private EntityManager entityManager;
    private Query query1;
    private Query queryById;

    public NoteRepositoryImpl() {
        entityManager = getEntityManagerFactory();
        String queryText = "SELECT n FROM Notes n";
        query1 = entityManager.createQuery(queryText);
        String queryText2 = "SELECT n FROM Notes n WHERE n.id = :id ";
        queryById = entityManager.createQuery(queryText2);
    }

    public Collection<Notes> getAllNotes() {
        Collection<Notes> list1 = query1.getResultList();
        Logger logger = Logger.getLogger(NoteRepositoryImpl.class.getName());
        logger.log(Level.INFO, "db size :" + list1.size());
        return list1;
    }

    public EntityManager getEntityManagerFactory() {
        Map properties = new HashMap();
        properties.put("openjpa.ConnectionURL", "jdbc:hsqldb:file:" + System.getProperty("netbeans.user") + "/en4j/db");
        System.out.println(properties.toString());
        EntityManager toReturn = javax.persistence.Persistence.createEntityManagerFactory("JpaEntitiesClassLibraryPU", properties).createEntityManager();
        return java.beans.Beans.isDesignTime() ? null : toReturn;
    }

    public Notes get(int id) {
        queryById.setParameter("id", id);
        return (Notes)queryById.getSingleResult();

        //throw new UnsupportedOperationException("Not supported yet.");
    }
}
