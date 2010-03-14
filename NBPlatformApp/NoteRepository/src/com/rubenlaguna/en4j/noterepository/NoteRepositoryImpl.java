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

import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.jaxb.generated.Resource;
import com.rubenlaguna.en4j.jpaentities.Notes;
import com.rubenlaguna.en4j.noteinterface.Note;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.jdbc.FetchMode;
import org.apache.openjpa.persistence.jdbc.JDBCFetchPlan;
import org.netbeans.api.progress.ProgressHandle;

/**
 *
 * @author ecerulm
 */
public class NoteRepositoryImpl implements NoteRepository {

    private final Logger LOG = Logger.getLogger(NoteRepositoryImpl.class.getName());
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public NoteRepositoryImpl() {
    }

    public Collection<Note> getAllNotes() {
        Collection<Note> toReturn = new ArrayList<Note>();


        Collection<Notes> listNotes = new ArrayList<Notes>();
        EntityManager entityManager = null;
        try {
            entityManager = Installer.getEntityManagerFactory().createEntityManager();
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "cannot even create an entitymanager", ex);
            return toReturn;
        }
        try {
            String queryText = "SELECT n FROM Notes n";
            Query query1 = entityManager.createQuery(queryText);
            listNotes.addAll(query1.getResultList());
        } catch (PersistenceException ex) {
//            LOG.warning("could not load the notes from the db. Is the module closing?");
            LOG.log(Level.WARNING, "Couldn't retrieve the record from the db due to", ex);
        } finally {
            //enough since there is no transaction in the try-block
            //see http://bit.ly/b0p3Wj
            entityManager.close();
        }
        for (Notes n : listNotes) {
            toReturn.add(fromNotes(n));
        }

        LOG.log(Level.INFO, "db size :" + toReturn.size());
        return toReturn;
    }

    private Note fromNotes(final Notes origNotes) {
        Note f = new NoteAdapter(origNotes);
        return f;
    }

    public Note get(int id) {
        return get(id, true);
    }

    public Note get(int id, boolean withContents) {
        EntityManager entityManager = Installer.getEntityManagerFactory().createEntityManager();
        Note toReturn = null;
        try {
            String queryText2 = "SELECT n FROM Notes n WHERE n.id = :id ";
            Query queryById = entityManager.createQuery(queryText2);
            queryById.setParameter("id", id);
            final Notes result = (Notes) queryById.getSingleResult();
            //entityManager.
            if (withContents) {
                result.getContent();
                result.getResources();
            }

            toReturn = fromNotes(result);
        } catch (PersistenceException ex) {
            LOG.log(Level.WARNING,"could not load the note from the db. Is the module closing?",ex);
        } finally {
            //enough since there is no transaction in the try-block
            //see http://bit.ly/b0p3Wj
            entityManager.close();
        }
        //the entity now is detached 
        return toReturn;
    }
    public Note getByGuid(String id, boolean withContents) {
        EntityManager entityManager = Installer.getEntityManagerFactory().createEntityManager();
        Note toReturn = null;
        try {
            String queryText2 = "SELECT n FROM Notes n WHERE n.guid = :id ";
            Query queryById = entityManager.createQuery(queryText2);
            queryById.setParameter("id", id);
            final Notes result = (Notes) queryById.getSingleResult();
            if (withContents) {
                result.getContent();
                result.getResources();
            }

            toReturn = fromNotes(result);
        } catch (PersistenceException ex) {
            LOG.warning("could not load the note from the db. Is the module closing?");
        } finally {
            //enough since there is no transaction in the try-block
            //see http://bit.ly/b0p3Wj
            entityManager.close();
        }
        //the entity now is detached
        return toReturn;
    }

    public void importEntries(InputStream in, ProgressHandle ph) throws InterruptedException {
        try {

            long start = System.currentTimeMillis();
            XMLInputFactory factory = XMLInputFactory.newInstance();
            System.out.println("factory:" + factory);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(in);
            JAXBContext jc;

            jc = JAXBContext.newInstance("com.rubenlaguna.en4j.jaxb.generated");

            Unmarshaller u = jc.createUnmarshaller();

            int notes = 0;
            for (int event = xmlStreamReader.next(); event
                    != XMLStreamConstants.END_DOCUMENT; event = xmlStreamReader.next()) {

                if (Thread.interrupted()) {
                    LOG.info("file import was CANCELLED");
                    xmlStreamReader.close();

                    throw new java.lang.InterruptedException();
                }
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        if ("note".equals(xmlStreamReader.getLocalName())) {

                            EntityManager entityManager = Installer.getEntityManagerFactory().createEntityManager();
                            try {
                                EntityTransaction t = entityManager.getTransaction();
                                try {
                                    t.begin();

                                    com.rubenlaguna.en4j.jaxb.generated.Note n = (com.rubenlaguna.en4j.jaxb.generated.Note) u.unmarshal(xmlStreamReader);
                                    ph.progress(n.getTitle());
                                    notes++;

                                    Notes entityNode = new Notes();
                                    entityNode.setContent(n.getContent());
                                    entityNode.setCreated(new Date());
                                    entityNode.setUpdated(new Date());
                                    entityNode.setTitle(n.getTitle());
                                    //TODO: add resources to the database
                                    List<Resource> resources = n.getResource();

                                    int i = 1;

                                    for (Resource r : resources) {
                                        if (null != r.getData()) {
                                            com.rubenlaguna.en4j.jpaentities.Resource resourceEntity = new com.rubenlaguna.en4j.jpaentities.Resource();

                                            byte[] data = r.getData().getValue();
                                            resourceEntity.setData(data);
                                            resourceEntity.setOwner(entityNode);
                                            entityNode.addResource(resourceEntity);
                                            entityManager.persist(resourceEntity);

                                        } else {
                                            LOG.info("resource " + i + "/" + resources.size() + " has no data?. note title is " + n.getTitle());

                                        }
                                        i++;

                                    } //for
                                    entityManager.persist(entityNode);
                                    entityManager.getTransaction().commit();
                                } finally {
                                    //we need to rollback the transaction if it
                                    //isn't commited otherwise the entityManager
                                    //.close() will leave the PersistenceContext
                                    // open see http://bit.ly/b0p3Wj
                                    if (t.isActive()) {
                                        t.rollback();
                                    }
                                }
                            } finally {
                                entityManager.close();
                            }
                        } //end if
                        break;
                } // end switch
            } // end for
            xmlStreamReader.close();

            long delta = System.currentTimeMillis() - start;
            LOG.info("Import took " + delta + " ms");
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }



    public int getHighestUSN() {

        EntityManager entityManager = Installer.getEntityManagerFactory().createEntityManager();

        Integer toReturn = null;
        try {
            Query highestUSNQuery = entityManager.createQuery("SELECT MAX(p.updateSequenceNumber) FROM Notes p");

            toReturn = (Integer) highestUSNQuery.getSingleResult();
        } finally {
            //enough since there is no transaction in the try-block
            //see http://bit.ly/b0p3Wj
            entityManager.close();
        }
        if (null == toReturn) {
            //there is no entries in the db
            toReturn = 0;

        }
        return toReturn;
    }

    public boolean add(Note n) {
        if (null==n){
            return false;
        }
        LOG.info("add note: " + n);
        EntityManager entityManager = null;
        try {
            entityManager = Installer.getEntityManagerFactory().createEntityManager();
        } catch (IllegalStateException ex) {
            LOG.warning("Couldn't add the note. Probably the module is closing.");
            return false;
        }
        boolean toReturn = true;//assume that it will work
        try {
            final EntityTransaction t = entityManager.getTransaction();

            try {
                t.begin();

                Notes entityToPersist = new Notes();

                entityToPersist.setGuid(n.getGuid());
                entityToPersist.setTitle(n.getTitle());
//                LOG.info("content: "+n.getContent().substring(0, 50));
                entityToPersist.setContent(n.getContent());
                entityToPersist.setCreated(n.getCreated());
                entityToPersist.setSourceurl(n.getSourceurl());
                entityToPersist.setUpdated(n.getUpdated());
                entityToPersist.setUpdateSequenceNumber(n.getUpdateSequenceNumber());

                for (Iterator<com.rubenlaguna.en4j.noteinterface.Resource> it = n.getResources().iterator(); it.hasNext();) {
                    com.rubenlaguna.en4j.noteinterface.Resource resource = it.next();
                    com.rubenlaguna.en4j.jpaentities.Resource resourceEntity = new com.rubenlaguna.en4j.jpaentities.Resource();

                    byte[] data = resource.getData();
                    resourceEntity.setData(data);
                    resourceEntity.setAlternateData(resource.getAlternateData());
                    resourceEntity.setAltitude(resource.getAltitude());
                    resourceEntity.setCameraMake(resource.getCameraMake());
                    resourceEntity.setCameraModel(resource.getCameraModel());
                    //resourceEntity.setClientWillIndex(toReturn);
                    resourceEntity.setFilename(resource.getFilename());
                    resourceEntity.setGuid(resource.getGuid());
                    resourceEntity.setLatitude(resource.getLatitude());
                    resourceEntity.setLongitude(resource.getLongitude());
                    resourceEntity.setMime(resource.getMime());
                    resourceEntity.setNoteguid(n.getGuid());
                    resourceEntity.setPremiumAttachment(resource.getPremiumAttachment());
                    resourceEntity.setRecognition(resource.getRecognition());
                    resourceEntity.setTimestamp(resource.getTimestamp());
                   

                    resourceEntity.setOwner(entityToPersist);
                    entityToPersist.addResource(resourceEntity);
                    entityManager.persist(resourceEntity);
                }

                entityManager.persist(entityToPersist);
                entityManager.getTransaction().commit();
            } catch (PersistenceException ex) {
//                LOG.log(Level.WARNING, "Could add note due to ...", ex);
//                LOG.warning("could not load the note from the db. Is the module closing?");
                  LOG.log(Level.WARNING,"could not load the note from the db. Is the module closing?",ex);
                toReturn = false;
            } finally {
                //we need to rollback the transaction if it
                //isn't commited otherwise the entityManager
                //.close() will leave the PersistenceContext
                // open see http://bit.ly/b0p3Wj

                if (t.isActive()) {
                    LOG.info("rollbacking transaction");
                    t.rollback();
                }
            }
        } finally {
            entityManager.close();
        }
        this.pcs.firePropertyChange("notes", null, null);
        return toReturn;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }
}
