/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.jaxb.generated.Resource;
import com.rubenlaguna.en4j.jpaentities.Notes;
import com.rubenlaguna.en4j.noteinterface.Note;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.netbeans.api.progress.ProgressHandle;

/**
 *
 * @author ecerulm
 */
public class NoteRepositoryImpl implements NoteRepository {

    private EntityManager entityManager;
    private Query query1;
    private Query queryById;

    public NoteRepositoryImpl() {
        entityManager = Installer.getEntityManager();
        String queryText = "SELECT n FROM Notes n";
        query1 = entityManager.createQuery(queryText);
        String queryText2 = "SELECT n FROM Notes n WHERE n.id = :id ";
        queryById = entityManager.createQuery(queryText2);
    }

    public Collection<Note> getAllNotes() {
        Collection<Notes> listNotes = query1.getResultList();
        Collection<Note> toReturn = new ArrayList<Note>();


        for (Notes n : listNotes) {
            toReturn.add(fromNotes(n));
        }

        Logger logger = Logger.getLogger(NoteRepositoryImpl.class.getName());
        logger.log(Level.INFO, "db size :" + toReturn.size());
        return toReturn;
    }

    private Note fromNotes(final Notes origNotes) {

//            InvocationHandler handler = new InvocationHandler() {
//
//                public Object invoke(Object proxy, Method method,
//                        Object[] args) throws Throwable {
//
//                    Method method2 = origNotes.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
//
//                    Object toReturn = method2.invoke(origNotes,args);
//
//                    //method.invoke(thisNote, args);
//                    return toReturn;
//                }
//            };
//            Note f = (Note) Proxy.newProxyInstance(this.getClass().getClassLoader(),
//                    new Class[]{Note.class},
//                    handler);

        Note f = new NoteAdapter(origNotes);
        return f;
    }

    public Note get(int id) {
        queryById.setParameter("id", id);

        return fromNotes((Notes) queryById.getSingleResult());
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void importEntries(InputStream in, ProgressHandle ph) {
        try {

            XMLInputFactory factory = XMLInputFactory.newInstance();
            System.out.println("factory:" + factory);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(in);
            JAXBContext jc;

            jc = JAXBContext.newInstance("com.rubenlaguna.en4j.jaxb.generated");

            Unmarshaller u = jc.createUnmarshaller();
            int inHeader = 0;
            int notes = 0;

            for (int event = xmlStreamReader.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlStreamReader.next()) {
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        if ("note".equals(xmlStreamReader.getLocalName())) {

                            entityManager.getTransaction().begin();

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

                            entityManager.persist(entityNode);

                            entityManager.getTransaction().commit();

                            for (Resource r : resources) {
                                entityManager.getTransaction().begin();

                                com.rubenlaguna.en4j.jpaentities.Resource resourceEntity = new com.rubenlaguna.en4j.jpaentities.Resource();
                                byte[] data = r.getData().getValue();

                                MessageDigest md5 = MessageDigest.getInstance("MD5");
                                BigInteger hash = new BigInteger(1, md5.digest(data));
                                String hashword = hash.toString(16);
                                resourceEntity.setHash(hashword);

                                resourceEntity.setData(data);
                                resourceEntity.setOwner(entityNode);
                                entityNode.addResource(resourceEntity);
                                entityManager.getTransaction().commit();

                            }

                        } //end if
                        break;

                } // end switch
            } // end for
            xmlStreamReader.close();

        } catch (Exception ex) {
            Logger.getLogger(NoteRepositoryImpl.class.getName()).log(Level.SEVERE, null, ex);
        }






    }
}
