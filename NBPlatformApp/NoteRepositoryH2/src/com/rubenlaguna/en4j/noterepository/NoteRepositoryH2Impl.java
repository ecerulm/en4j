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
import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.NoteReader;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Exceptions;
//import com.rubenlaguna.en4j.hsqldbnoterep.Installer;

/**
 *
 * @author ecerulm
 */
public class NoteRepositoryH2Impl implements NoteRepository {

    private final Logger LOG = Logger.getLogger(NoteRepositoryH2Impl.class.getName());
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private Connection connection = null;
    private final Map softrefMapById = new SoftHashMap();
    private final Map softrefMapByGuid = new SoftHashMap();
    private final Map resSoftMapByGuid = new SoftHashMap();

    public NoteRepositoryH2Impl() {
        //            c.createStatement().execute(
        //                  "CREATE CACHED TABLE NOTES ("
        //                        + "ID INTEGER NOT NULL, "
        //                        + "ISACTIVE BIT NOT NULL, "
        //                        + "GUID VARCHAR(36) NOT NULL,"
        //                        + "SERIALIZEDOBJECT OTHER, "
        //                        + "PRIMARY KEY (ID), "
        //                        + "CONSTRAINT UNQ_GUID UNIQUE (GUID))");
        this.connection = com.rubenlaguna.en4j.noterepository.Installer.c;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    public Collection<Note> getAllNotes() {
        List<Note> toReturn = new ArrayList<Note>();
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT SERIALIZEDOBJECT FROM NOTES");
            while (rs.next()) {
                toReturn.add((Note) rs.getObject("SERIALIZEDOBJECT"));
            }
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }
        return toReturn;
    }

    public void importEntries(InputStream in, ProgressHandle ph) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Note get(int id) {
        final Note cached = (Note) softrefMapById.get(id);
        if (null != cached) {
            LOG.info("cache hit note id:"+id);
            return cached;
        }
        try {
            PreparedStatement pstmt = connection.prepareStatement("SELECT SERIALIZEDOBJECT FROM NOTES WHERE ID = ?");
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                LOG.info("No entry found with id = " + id);
                return null;
            }
            final Note toReturn = (Note) rs.getObject("SERIALIZEDOBJECT");
            softrefMapById.put(id,toReturn);
            return toReturn;
        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
            return null;
        }
    }

    public Note get(int id, boolean withContents) {
        return get(id);
    }

    public Note getByGuid(String guid, boolean withContents) {
        final Note cached = (Note) softrefMapByGuid.get(guid);
        if (null != cached) {
            LOG.info("cache hit note guid:"+guid);
            return cached;
        }
        try {
            LOG.info("searching note with guid: " + guid);
            PreparedStatement stmt = connection.prepareStatement("SELECT SERIALIZEDOBJECT FROM NOTES WHERE GUID = ?");
            stmt.setString(1, guid);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                LOG.info("There is no entry in the db  with guid: " + guid);
                return null;
            }
            final Note toReturn = (Note) rs.getObject("SERIALIZEDOBJECT");
            softrefMapByGuid.put(guid,toReturn);
            return toReturn;
        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
            return null;
        }
    }

    public boolean isNoteUpToDate(String guid, int usn) {
        final Note note = getByGuid(guid, true);
        if (null == note) {
            return false;
        }
        return note.getUpdateSequenceNumber() >= usn;
    }

    public synchronized boolean add(NoteReader note) {


        //first iterate over resources and
        for (Resource resource : note.getResources()) {
            if (!insertResource(resource)) {
                return false;
            }
        }
        // and then add the note
        if (!insertNote(note)) {
            return false;
        }

        this.pcs.firePropertyChange("notes", null, null);
        return true;
    }

    private boolean insertNote(NoteReader note) {
        try {
            NoteImpl n = new NoteImpl(note);
            String guid = n.getGuid();
            PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM NOTES WHERE GUID=?");
            deleteStmt.setString(1, guid);
            deleteStmt.executeUpdate();
            PreparedStatement insertStmt = this.connection.prepareStatement("INSERT INTO NOTES VALUES(NULL,?,?,?,?)");
            insertStmt.setBoolean(1, n.isActive());
            insertStmt.setString(2, guid);
            insertStmt.setObject(3, n);
            insertStmt.setString(4, note.getContent());
            final int rowCount = insertStmt.executeUpdate();
            if (rowCount != 1) {
                return false;
            }
            return true;
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
    }

    private boolean insertResource(Resource resource) {
        try {
            ResourceImpl r = new ResourceImpl(resource);
            String guid = r.getGuid();
            PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM RESOURCES WHERE GUID=?");
            deleteStmt.setString(1, guid);
            deleteStmt.executeUpdate();
            PreparedStatement insertStmt = this.connection.prepareStatement("INSERT INTO RESOURCES VALUES(?,?,?,?,?)");
            insertStmt.setString(1, guid);
            insertStmt.setObject(2, r);
            insertStmt.setString(3, r.getNoteguid());
            insertStmt.setString(4, r.getDataHash());
            insertStmt.setBytes(5, resource.getData());
            LOG.info("inserting resource guid:" + guid + " size:" + resource.getData().length);

            final int rowCount = insertStmt.executeUpdate();
            if (rowCount == 1) {
                return true;
            }
            return false;
        } catch (SQLException sQLException) {
            LOG.log(Level.WARNING, "exception while trying to insert resource " + resource.getGuid() + " size:" + resource.getData().length, sQLException);
//            Exceptions.printStackTrace(sQLException);
            return false;
        }
    }

    public Resource getResource(String guid, String hash) {
        final Resource cached = (Resource) resSoftMapByGuid.get(guid+hash);
        if (null != cached) {
            LOG.info("cache hit resource parent guid:"+guid+" hash:"+hash);
            return cached;
        }

        try {
            LOG.info("searching resource with parent guid: " + guid + " and hash: " + hash);
            PreparedStatement pstmt = connection.prepareStatement("SELECT SERIALIZEDOBJECT FROM RESOURCES WHERE OWNERGUID=? AND HASH=?");
            pstmt.setString(1, guid);
            pstmt.setString(2, hash);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                LOG.info("There is no entry in the db  with guid: " + guid);
                return null;
            }
            final Resource toReturn = (Resource) rs.getObject("SERIALIZEDOBJECT");
            resSoftMapByGuid.put(guid+hash, toReturn);
            return toReturn;
        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
            return null;
        }
    }
}
