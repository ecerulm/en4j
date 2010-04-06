/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.NoteReader;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.nio.CharBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class NoteImpl implements Note, Serializable {

    private final String guid;
    private final Date created;
    private final Date deleted;
    private final Collection<String> resourcesHashes = new ArrayList<String>();
    private final String sourceurl;
    private final String title;
    private final int usn;
    private final Date updated;
    private final boolean active;

    NoteImpl(NoteReader note) {
        this.guid = note.getGuid();
        this.created = note.getCreated();
        this.deleted = note.getDeleted();
        for (Resource r : note.getResources()) {
            this.resourcesHashes.add(r.getDataHash());
        }
        this.sourceurl = note.getSourceurl();
        this.title = note.getTitle();
        this.usn = note.getUpdateSequenceNumber();
        this.updated = note.getUpdated();
        this.active = note.isActive();
    }

    public String getContent() {
        final Reader characterStream = getContentAsReader();
        if (null == characterStream) {
            return "";
        }
        CharBuffer cb = CharBuffer.allocate(64000);
        StringBuffer sb = new StringBuffer();
        try {
            while (characterStream.ready()) {
                cb.clear();
                characterStream.read(cb);
                cb.flip();
                sb.append(cb);
            }
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "caught exception:", e);
        } finally {
            try {
                characterStream.close();
            } catch (IOException e) {
            }
        }
        return sb.toString();
    }

    public Reader getContentAsReader() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT CONTENT FROM NOTES WHERE GUID=?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final Reader characterStream = rs.getCharacterStream("CONTENT");
                return characterStream;
            }
        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
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
        return null;
    }

    public void setContent(String content) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getCreated() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCreated(Date created) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Integer getId() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT ID FROM NOTES WHERE GUID =?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final int toReturn = rs.getInt("ID");
                return toReturn;
            }
        } catch (SQLException sQLException) {
            getLogger().log(Level.WARNING, "exception caught:", sQLException);
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

        return -1;

    }

    public void setId(Integer id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getSourceurl() {
        return this.sourceurl;
    }

    public void setSourceurl(String sourceurl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getUpdated() {
        return this.updated;
    }

    public void setUpdated(Date updated) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getUpdateSequenceNumber() {
        return this.usn;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Date deleted) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Resource getResource(String hash) {
        return Lookup.getDefault().lookup(NoteRepositoryH2Impl.class).getResource(guid, hash);
    }

    public Collection<Resource> getResources() {
        List<Resource> toReturn = new ArrayList<Resource>();
        for (String hash : resourcesHashes) {
            toReturn.add(getResource(hash));
        }
        return toReturn;
    }

    public String getGuid() {
        return this.guid;
    }

    private Connection getConnection() {
        return Installer.c;
    }

    private Logger getLogger() {
        return Logger.getLogger(NoteImpl.class.getName());
    }
}
