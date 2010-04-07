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
class NoteImpl implements Note {

    private final int id;

    NoteImpl(int id) {
        this.id = id;
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
        ResultSet rs = null;
        try {
            synchronized (Installer.selectContentById) {
                Installer.selectContentById.setInt(1, id);
                rs = Installer.selectContentById.executeQuery();
            }
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
        return id;
    }

    public void setId(Integer id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getSourceurl() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT SOURCEURL FROM NOTES WHERE ID =?");
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final String toReturn = rs.getString("SOURCEURL");
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
        return "";

    }

    public void setSourceurl(String sourceurl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getTitle() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT TITLE FROM NOTES WHERE ID =?");
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final String toReturn = rs.getString("TITLE");
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
        return "";
    }

    public void setTitle(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getUpdated() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setUpdated(Date updated) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getUpdateSequenceNumber() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT USN FROM NOTES WHERE ID =?");
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final int toReturn = rs.getInt("USN");
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

    public boolean isActive() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setActive(boolean active) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getDeleted() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setDeleted(Date deleted) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Resource getResource(String hash) {
        return Lookup.getDefault().lookup(NoteRepositoryH2Impl.class).getResource(getGuid(), hash);
    }

    public Collection<Resource> getResources() {
        List<Resource> toReturn = new ArrayList<Resource>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT HASH FROM RESOURCES WHERE OWNERGUID =?");
            pstmt.setString(1, getGuid());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                final String hash = rs.getString("HASH");
                toReturn.add(getResource(hash));
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
        return toReturn;
    }

    public String getGuid() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT GUID FROM NOTES WHERE ID =?");
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final String toReturn = rs.getString("GUID");
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
        return "";
    }

    private Connection getConnection() {
        return Installer.c;
    }

    private Logger getLogger() {
        return Logger.getLogger(NoteImpl.class.getName());
    }
}
