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
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = connection.prepareStatement("SELECT ID FROM NOTES");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                final int id = rs.getInt("ID");
                toReturn.add(get(id));
            }
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }

            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }
        return toReturn;
    }

    public void importEntries(InputStream in, ProgressHandle ph) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Note get(int id) {
        final Note cached = (Note) softrefMapById.get(id);
        if (null != cached) {
            LOG.fine("cache hit note id:" + id);
            return cached;
        }
        final Note toReturn = new NoteImpl(id);
        softrefMapById.put(id, toReturn);
        return toReturn;
    }

    public Note get(int id, boolean withContents) {
        return get(id);
    }

    public Note getByGuid(String guid, boolean withContents) {
        final Note cached = (Note) softrefMapByGuid.get(guid);
        if (null != cached) {
            LOG.fine("cache hit note guid:" + guid);
            return cached;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            LOG.info("searching note with guid: " + guid);
            pstmt = connection.prepareStatement("SELECT ID FROM NOTES WHERE GUID = ?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                LOG.info("There is no entry in the db  with guid: " + guid);
                return null;
            }
//            final Note toReturn = (Note) rs.getObject("SERIALIZEDOBJECT");
            final int id = rs.getInt("ID");
            final Note toReturn = get(id);
            softrefMapByGuid.put(guid, toReturn);
            return toReturn;
        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
            return null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }

            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
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
        PreparedStatement deleteStmt = null;
        PreparedStatement insertStmt = null;
        try {
            String guid = note.getGuid();

            deleteStmt = connection.prepareStatement("DELETE FROM NOTES WHERE GUID=?");
            deleteStmt.setString(1, guid);
            deleteStmt.executeUpdate();
            insertStmt = this.connection.prepareStatement("INSERT INTO NOTES (ID,ISACTIVE,GUID,CONTENT,TITLE,SOURCEURL,USN) VALUES(NULL,?,?,?,?,?,?)");
            insertStmt.setBoolean(1, note.isActive());
            insertStmt.setString(2, guid);
            insertStmt.setCharacterStream(3, note.getContentAsReader());
            insertStmt.setString(4, note.getTitle());
            insertStmt.setString(5, note.getSourceurl());
            insertStmt.setInt(6, note.getUpdateSequenceNumber());

            final int rowCount = insertStmt.executeUpdate();
            if (rowCount != 1) {
                return false;
            }
            return true;
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
            return false;
        } finally {
            if (null != insertStmt) {
                try {
                    insertStmt.close();
                } catch (SQLException ex) {
                }
            }
            if (null != deleteStmt) {
                try {
                    deleteStmt.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    private boolean insertResource(Resource resource) {
        PreparedStatement deleteStmt = null;
        PreparedStatement insertStmt = null;
        try {
            String guid = resource.getGuid();
            deleteStmt = connection.prepareStatement("DELETE FROM RESOURCES WHERE GUID=?");
            deleteStmt.setString(1, guid);
            deleteStmt.executeUpdate();
            insertStmt = this.connection.prepareStatement("INSERT INTO RESOURCES (GUID,OWNERGUID,HASH,DATA,FILENAME,MIME,RECOGNITION) VALUES(?,?,?,?,?,?,?)");
            insertStmt.setString(1, guid);
            insertStmt.setString(2, resource.getNoteguid());
            insertStmt.setString(3, resource.getDataHash());
            insertStmt.setBinaryStream(4, resource.getDataAsInputStream());
            insertStmt.setString(5, resource.getFilename());
            insertStmt.setString(6, resource.getMime());
            insertStmt.setBytes(7, resource.getRecognition());
            LOG.info("inserting resource guid:" + guid);

            final int rowCount = insertStmt.executeUpdate();
            if (rowCount == 1) {
                return true;
            }
            return false;
        } catch (SQLException sQLException) {
            LOG.log(Level.WARNING, "exception while trying to insert resource " + resource.getGuid(), sQLException);
            return false;
        } finally {
            if (null != deleteStmt) {
                try {
                    deleteStmt.close();
                } catch (SQLException ex) {
                }
            }
            if (null != insertStmt) {
                try {
                    insertStmt.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    public Resource getResource(String guid, String hash) {
        final Resource cached = (Resource) resSoftMapByGuid.get(guid + hash);
        if (null != cached) {
            LOG.info("cache hit resource parent guid:" + guid + " hash:" + hash);
            return cached;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            LOG.fine("searching resource with parent guid: " + guid + " and hash: " + hash);
            pstmt = connection.prepareStatement("SELECT GUID FROM RESOURCES WHERE OWNERGUID=? AND HASH=?");
            pstmt.setString(1, guid);
            pstmt.setString(2, hash);
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                LOG.info("There is no entry in the db  with guid: " + guid);
                return null;
            }
            final String resguid = rs.getString("GUID");
            final Resource toReturn = new ResourceImpl(resguid);
            resSoftMapByGuid.put(guid + hash, toReturn);
            return toReturn;
        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
            return null;
        } finally {
            if (null != rs) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                }
            }
            if (null != pstmt) {
                try {
                    pstmt.close();
                } catch (SQLException ex) {
                }
            }
        }
    }
}
