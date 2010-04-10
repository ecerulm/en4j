/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public class DbPstmts {

    private static final Logger LOG = Logger.getLogger(DbPstmts.class.getName());
    private static DbPstmts theInstance = null;
    private static boolean closed = false;
    PreparedStatement sourceurlPstmt = null;
    PreparedStatement contentFromNotesPstmt = null;

    private DbPstmts() {
    }

    public Reader getContentAsReader(int id) {
        ResultSet rs = null;
        try {
            if (contentFromNotesPstmt == null) {
                contentFromNotesPstmt = getConnection().prepareStatement("SELECT CONTENT FROM NOTES WHERE ID=?");
            }
            synchronized (contentFromNotesPstmt) {
                contentFromNotesPstmt.setInt(1, id);
                rs = contentFromNotesPstmt.executeQuery();
                if (rs.next()) {
                    final Reader characterStream = rs.getCharacterStream("CONTENT");
                    return characterStream;
                }
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

    public String getSourceurl(int id) {
        ResultSet rs = null;
        try {

            if (sourceurlPstmt == null) {
                sourceurlPstmt = getConnection().prepareStatement("SELECT SOURCEURL FROM NOTES WHERE ID =?");
            }
            sourceurlPstmt.setInt(1, id);
            rs = sourceurlPstmt.executeQuery();
            if (rs.next()) {
                final String toReturn = rs.getString("SOURCEURL");
                return toReturn;
            }
        } catch (SQLException sQLException) {
            LOG.log(Level.WARNING, "exception caught:", sQLException);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }

            }
        }
        return "";
    }

    public String getTitle(int id) {
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
            LOG.log(Level.WARNING, "exception caught:", sQLException);
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

    public int getUpdateSequenceNumber(int id) {
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
            LOG.log(Level.WARNING, "exception caught:", sQLException);
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

    public Collection<String> getResources(String guid) {
        List<String> toReturn = new ArrayList<String>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT HASH FROM RESOURCES WHERE OWNERGUID =?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                final String hash = rs.getString("HASH");
                toReturn.add(hash);
            }
        } catch (SQLException sQLException) {
            LOG.log(Level.WARNING, "exception caught:", sQLException);
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

    public String getGuid(int id) {
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
            LOG.log(Level.WARNING, "exception caught:", sQLException);
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

    public synchronized void close() {
        closed = true;
        theInstance = null;

        if (contentFromNotesPstmt != null) {
            try {
                contentFromNotesPstmt.close();
            } catch (SQLException e) {
            }
        }
        if (sourceurlPstmt != null) {
            try {
                sourceurlPstmt.close();
            } catch (SQLException e) {
            }
        }
    }

    private Connection getConnection() {
        return Installer.c;
    }

    public synchronized static DbPstmts getInstance() {
        if (theInstance == null && !closed) {
            theInstance = new DbPstmts();
        }
        return theInstance;
    }
}
