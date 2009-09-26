/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.mainmodule;


import com.rubenlaguna.en4j.jaxb.generated.Note;
import com.rubenlaguna.en4j.jpaentities.Notes;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.Exceptions;

public final class ImportEvernoteFile implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        // TODO implement action body
        //The default dir to use if no value is stored
        File home = new File(System.getProperty("user.home") + File.separator + "lib");
        //Now build a file chooser and invoke the dialog in one line of code
        //"libraries-dir" is our unique key
        File toAdd = new FileChooserBuilder("import-dir").setTitle("Import Evernote File").setDefaultWorkingDirectory(home).setApproveText("Add").showOpenDialog();
        //Result will be null if the user clicked cancel or closed the dialog w/o OK
        if (toAdd != null) {
            //do something
        }

        if (toAdd != null) {
            try {
                final ProgressHandle ph = ProgressHandleFactory.createHandle("import");
                ph.start();

                EntityManager entityManager1 = javax.persistence.Persistence.createEntityManagerFactory("JpaEntitiesClassLibraryPU").createEntityManager();
                int available = (int) toAdd.length();
                ph.switchToDeterminate(available);
                InputStream in = null;
                in = new BufferedInputStream(new ProgressInputStream(new FileInputStream(toAdd), available / 100, new ProgressListener() {

                    public void progress(int i) {
                        ph.progress(i);
                    }
                }));

                XMLInputFactory factory = XMLInputFactory.newInstance();
                System.out.println("factory:" + factory);
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
                XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(in);
                JAXBContext jc = JAXBContext.newInstance("com.rubenlaguna.en4j.jaxb.generated");
                Unmarshaller u = jc.createUnmarshaller();
                int inHeader = 0;
                int notes = 0;
                for (int event = xmlStreamReader.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlStreamReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT:
                            if ("note".equals(xmlStreamReader.getLocalName())) {
                                entityManager1.getTransaction().begin();

                                Note n = (Note) u.unmarshal(xmlStreamReader);
                                ph.progress(n.getTitle());
                                notes++;
                                Notes entityNode = new Notes();
                                entityNode.setContent(n.getContent());
                                entityNode.setCreated(new Date());
                                entityNode.setUpdated(new Date());
                                entityNode.setTitle(n.getTitle());
                                entityManager1.persist(entityNode);
                                entityManager1.getTransaction().commit();
                                System.out.println("persisted: " + n.getTitle());
                            } //end if
                            break;

                    } // end switch
                } // end for
                xmlStreamReader.close();
                ph.finish();
            } catch (JAXBException ex) {
                Exceptions.printStackTrace(ex);
            } catch (FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (XMLStreamException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        NoteListTopComponent.findInstance().invalidate();
    }
}

