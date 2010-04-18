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

import com.rubenlaguna.en4j.interfaces.NoteFinder;
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
import org.openide.util.Lookup;
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
    private final Map resSoftMapByOwnerGuidAndHash = new SoftHashMap();
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
        long start = System.currentTimeMillis();
        List<Note> toReturn = new ArrayList<Note>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {

            pstmt = connection.prepareStatement("SELECT ID FROM NOTES WHERE ISACTIVE = ?");
            pstmt.setBoolean(1, true);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                final int id = rs.getInt("ID");
                toReturn.add(get(id));
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "caught exception:" + ex.getMessage());
            LOG.log(Level.FINE, "caught exception:", ex);
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
        long delta = System.currentTimeMillis() - start;
        Installer.mbean.sampleGetAllNotes(delta);
        return toReturn;
    }

    public void importEntries(InputStream in, ProgressHandle ph) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Note get(int id) {
        long start = System.currentTimeMillis();
        final Note cached = (Note) softrefMapById.get(id);
        if (null != cached) {
            LOG.fine("cache hit note id:" + id);
            if (cached.getGuid() != null) {
                return cached;
            } else {
                LOG.warning("we got something from the cache (id:" + id + ")but seems to be corrupted (no guid)");
                softrefMapById.remove(id);
            }
        }
        final Note toReturn = new NoteImpl(id);
        if (toReturn.getGuid() == null) {
            LOG.info("Better return null than a non existing entry");
            Lookup.getDefault().lookup(NoteFinder.class).remove(toReturn);
            return null;
        }
        softrefMapById.put(id, toReturn);
        long delta = System.currentTimeMillis() - start;
        Installer.mbean.sampleGetNote(delta);
        return toReturn;
    }

    public Note get(int id, boolean withContents) {
        return get(id);
    }

    public Note getByGuid(String guid, boolean withContents) {
        long start = System.currentTimeMillis();
        final Note cached = (Note) softrefMapByGuid.get(guid);
        if (null != cached) {
            LOG.fine("cache hit note guid:" + guid);
            if (cached.getGuid() != null) {
                return cached;
            } else {
                LOG.warning("we got something from the cache (guid:" + guid + ")but seems to be corrupted (no guid)");
                softrefMapByGuid.remove(guid);
            }
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
            long delta = System.currentTimeMillis() - start;
            Installer.mbean.sampleGetNote(delta);
            return toReturn;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "caught exception:" + ex.getMessage());
            LOG.log(Level.FINE, "caught exception:", ex);
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

    public boolean isResourceUpToDate(String guid, int usn) {
        final Resource res = getResourceByGuid(guid);
        if (null == res) {
            return false;
        }
        return res.getUpdateSequenceNumber() >= usn;
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

    @Override
    public synchronized boolean deleteNoteByGuid(String noteguid) {
        Note note = getByGuid(noteguid, true);
        for (Resource r : note.getResources()) {
            deleteResourceByGuid(r.getGuid());
        }
        //Frst remove it from caches
        final Note cached = (Note) softrefMapByGuid.get(noteguid);
        if (null != cached) {
            softrefMapById.remove(cached.getId());
        }
        softrefMapByGuid.remove(noteguid);
        //Then remove it from the database
        PreparedStatement pstmt = null;
        try {
            LOG.info("delete note with guid: " + noteguid);
            pstmt = connection.prepareStatement("DELETE FROM NOTES WHERE GUID = ?");
            pstmt.setString(1, noteguid);
            int rows = pstmt.executeUpdate();
            if (rows < 1) {
                LOG.info("There is no note in the db  with guid: " + noteguid + " so I cannot delete");
                return false;
            }
            return true;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "caught exception:" + ex.getMessage());
            LOG.log(Level.FINE, "caught exception:", ex);
            return false;
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public synchronized boolean add(Resource res) {
        boolean toReturn = insertResource(res);
        this.pcs.firePropertyChange("notes", null, null);
        return toReturn;
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
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "caught exception:" + ex.getMessage());
            LOG.log(Level.FINE, "caught exception:", ex);
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
            insertStmt = this.connection.prepareStatement("INSERT INTO RESOURCES (GUID,OWNERGUID,HASH,DATA,FILENAME,MIME,RECOGNITION,USN) VALUES(?,?,?,?,?,?,?,?)");
            insertStmt.setString(1, guid);
            insertStmt.setString(2, resource.getNoteguid());
            insertStmt.setString(3, resource.getDataHash());
            insertStmt.setBinaryStream(4, resource.getDataAsInputStream());
            insertStmt.setString(5, resource.getFilename());
            insertStmt.setString(6, resource.getMime());
            insertStmt.setBytes(7, resource.getRecognition());
            insertStmt.setInt(8, resource.getUpdateSequenceNumber());
            LOG.info("inserting resource guid:" + guid);

            final int rowCount = insertStmt.executeUpdate();
            if (rowCount == 1) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "caught exception:" + ex.getMessage());
            LOG.log(Level.FINE, "caught exception:", ex);
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
        final Resource cached = (Resource) resSoftMapByOwnerGuidAndHash.get(guid + hash);
        if (null != cached) {
            LOG.fine("cache hit resource parent guid:" + guid + " hash:" + hash);
            if (cached.getGuid() != null) {
                return cached;
            } else {
                LOG.warning("found resource (guid:" + guid + ",hash:" + hash + ") in the cache but it seems to be corrupted (no guid)");
                resSoftMapByOwnerGuidAndHash.remove(guid + hash);
            }
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
            resSoftMapByOwnerGuidAndHash.put(guid + hash, toReturn);
            return toReturn;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "caught exception:" + ex.getMessage());
            LOG.log(Level.FINE, "caught exception:", ex);
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

    private Resource getResourceByGuid(String resguid) {
        final Resource cached = (Resource) resSoftMapByGuid.get(resguid);
        if (null != cached) {
            LOG.fine("cache hit resourceguid:" + resguid);
            if (cached.getGuid() != null) {
                return cached;
            } else {
                LOG.warning("found resource (guid:" + resguid + ") in the cache but it seems to be corrupted");
                resSoftMapByGuid.remove(resguid);
            }
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            LOG.fine("searching resource with guid: " + resguid);
            pstmt = connection.prepareStatement("SELECT GUID FROM RESOURCES WHERE GUID=?");
            pstmt.setString(1, resguid);
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                LOG.info("There is no resource in the db  with guid: " + resguid);
                return null;
            }
            final Resource toReturn = new ResourceImpl(resguid);
            resSoftMapByGuid.put(resguid, toReturn);
            return toReturn;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "caught exception:" + ex.getMessage());
            LOG.log(Level.FINE, "caught exception:", ex);
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

    private boolean deleteResourceByGuid(String guid) {
        PreparedStatement deleteStmt = null;
        try {
            deleteStmt = connection.prepareStatement("DELETE FROM RESOURCES WHERE GUID=?");
            deleteStmt.setString(1, guid);
            final int rowCount = deleteStmt.executeUpdate();

            if (rowCount > 0) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "exception while trying to delete resource " + guid, ex);
            LOG.log(Level.FINE, "caught exception:", ex);
            return false;
        } finally {
            if (null != deleteStmt) {
                try {
                    deleteStmt.close();
                } catch (SQLException ex) {
                }
            }
        }
    }
}
