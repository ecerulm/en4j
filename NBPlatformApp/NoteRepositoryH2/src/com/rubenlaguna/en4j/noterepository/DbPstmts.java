/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
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
    PreparedStatement sourceurlPstmt;
    PreparedStatement contentFromNotesPstmt;
    PreparedStatement dataFromResourcesPstmt;
    PreparedStatement guidFromNotesPstmt;
    PreparedStatement recogFromResourcesPstmt;
    PreparedStatement ownerguidFromResourcesPstmt;
    PreparedStatement mimeFromResources;
    PreparedStatement hashResourcesOwnerPstmt;
    PreparedStatement usnFromNotesPstmt;
    PreparedStatement filenameFromResourcesPstmt;
    PreparedStatement hashFromResourcesPstmt;
    PreparedStatement titleFromNotes;

    private DbPstmts() throws SQLException {
        contentFromNotesPstmt = getConnection().prepareStatement("SELECT CONTENT FROM NOTES WHERE ID=?");
        sourceurlPstmt = getConnection().prepareStatement("SELECT SOURCEURL FROM NOTES WHERE ID =?");
        titleFromNotes = getConnection().prepareStatement("SELECT TITLE FROM NOTES WHERE ID =?");
        usnFromNotesPstmt = getConnection().prepareStatement("SELECT USN FROM NOTES WHERE ID =?");
        hashResourcesOwnerPstmt = getConnection().prepareStatement("SELECT HASH FROM RESOURCES WHERE OWNERGUID =?");
        guidFromNotesPstmt = getConnection().prepareStatement("SELECT GUID FROM NOTES WHERE ID =?");
        dataFromResourcesPstmt = getConnection().prepareStatement("SELECT DATA FROM RESOURCES WHERE GUID=?");
        dataFromResourcesPstmt = getConnection().prepareStatement("SELECT DATA FROM RESOURCES WHERE GUID=?");
        mimeFromResources = getConnection().prepareStatement("SELECT MIME FROM RESOURCES WHERE GUID=?");
        ownerguidFromResourcesPstmt = getConnection().prepareStatement("SELECT OWNERGUID FROM RESOURCES WHERE GUID=?");
        recogFromResourcesPstmt = getConnection().prepareStatement("SELECT RECOGNITION FROM RESOURCES WHERE GUID=?");
        filenameFromResourcesPstmt = getConnection().prepareStatement("SELECT FILENAME FROM RESOURCES WHERE GUID=?");
    }

    public Reader getContentAsReader(int id) {
        ResultSet rs = null;
        try {
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

            synchronized (sourceurlPstmt) {
                sourceurlPstmt.setInt(1, id);
                rs = sourceurlPstmt.executeQuery();
                if (rs.next()) {
                    final String toReturn = rs.getString("SOURCEURL");
                    return toReturn;
                }
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

        ResultSet rs = null;
        try {
            synchronized (titleFromNotes) {
                titleFromNotes.setInt(1, id);
                rs = titleFromNotes.executeQuery();
                if (rs.next()) {
                    final String toReturn = rs.getString("TITLE");
                    return toReturn;
                }
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

    public int getUpdateSequenceNumber(int id) {
        ResultSet rs = null;
        try {
            synchronized (usnFromNotesPstmt) {
                usnFromNotesPstmt.setInt(1, id);
                rs = usnFromNotesPstmt.executeQuery();
                if (rs.next()) {
                    final int toReturn = rs.getInt("USN");
                    return toReturn;
                }
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
        return -1;
    }

    public Collection<String> getResources(String guid) {
        List<String> toReturn = new ArrayList<String>();
        ResultSet rs = null;
        try {
            synchronized (hashResourcesOwnerPstmt) {
                hashResourcesOwnerPstmt.setString(1, guid);
                rs = hashResourcesOwnerPstmt.executeQuery();
                while (rs.next()) {
                    final String hash = rs.getString("HASH");
                    toReturn.add(hash);
                }
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
        return toReturn;
    }

    public String getGuid(int id) {
        ResultSet rs = null;
        try {
            synchronized (guidFromNotesPstmt) {
                guidFromNotesPstmt.setInt(1, id);
                rs = guidFromNotesPstmt.executeQuery();
                if (rs.next()) {
                    final String toReturn = rs.getString("GUID");
                    return toReturn;
                }
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

    public String getDataHash(String resGuid) {

        ResultSet rs = null;
        try {
            synchronized (hashFromResourcesPstmt) {
                hashFromResourcesPstmt.setString(1, resGuid);
                rs = hashFromResourcesPstmt.executeQuery();
                if (rs.next()) {
                    final String hash = rs.getString("HASH");
                    return hash;
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
        return "";
    }

    public InputStream getDataAsInputStream(String guid) {
        getLogger().fine("resource guid:" + guid);
        ResultSet rs = null;
        try {
            synchronized (dataFromResourcesPstmt) {
                dataFromResourcesPstmt.setString(1, guid);
                rs = dataFromResourcesPstmt.executeQuery();
                if (rs.next()) {
                    final Blob blob = rs.getBlob("DATA");
                    final InputStream is = blob.getBinaryStream();
                    return is;
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

    public byte[] getData(String resGuid) {
        getLogger().fine("resource guid:" + resGuid);
        ResultSet rs = null;
        try {

            int bloblength = -1;
            InputStream is = null;
            synchronized (dataFromResourcesPstmt) {
                dataFromResourcesPstmt.setString(1, resGuid);
                rs = dataFromResourcesPstmt.executeQuery();

                if (rs.next()) {
                    final Blob blob = rs.getBlob("DATA");
                    is = blob.getBinaryStream();
                    bloblength = (int) blob.length();
                }
            }
            // Create the byte array to hold the data
            final byte[] toReturn = new byte[bloblength];

            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            try {
                while (offset < toReturn.length
                        && (numRead = is.read(toReturn, offset, toReturn.length - offset)) >= 0) {
                    offset += numRead;
                }

                // Ensure all the bytes have been read in
                if (offset < toReturn.length) {
                    getLogger().warning("could not completely read data for resource guid:" + resGuid);
                    //throw new IOException("Could not completely read file " + file.getName());
                }
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "caught exception:", e);
            } finally {
                // Close the input stream and return bytes
                try {
                    is.close();
                } catch (IOException e) {
                }

            }
            return toReturn;

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

    public String getFilename(String resGuid) {
        ResultSet rs = null;
        try {

            synchronized (filenameFromResourcesPstmt) {
                filenameFromResourcesPstmt.setString(1, resGuid);
                rs = filenameFromResourcesPstmt.executeQuery();
                if (rs.next()) {
                    final String hash = rs.getString("FILENAME");
                    return hash;
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

        return "";
    }

    public String getMime(String guid) {
        ResultSet rs = null;
        try {

            synchronized (mimeFromResources) {
                mimeFromResources.setString(1, guid);
                rs = mimeFromResources.executeQuery();
                if (rs.next()) {
                    final String hash = rs.getString("MIME");
                    return hash;
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

        return "";

    }

    public String getNoteguid(String resGuid) {
        ResultSet rs = null;
        try {
            synchronized (ownerguidFromResourcesPstmt) {
                ownerguidFromResourcesPstmt.setString(1, resGuid);
                rs = ownerguidFromResourcesPstmt.executeQuery();
                if (rs.next()) {
                    final String hash = rs.getString("OWNERGUID");
                    return hash;
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

        return "";

    }

    public byte[] getRecognition(String resGuid) {
        ResultSet rs = null;
        try {
            synchronized (recogFromResourcesPstmt) {
                recogFromResourcesPstmt.setString(1, resGuid);
                rs = recogFromResourcesPstmt.executeQuery();
                if (rs.next()) {
                    final byte[] toReturn = rs.getBytes("RECOGNITION");
                    return toReturn;
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

    public synchronized void close() {
        closed = true;
        theInstance = null;
        closePStatement(contentFromNotesPstmt);
        contentFromNotesPstmt = null;
        closePStatement(sourceurlPstmt);
        sourceurlPstmt = null;
        closePStatement(dataFromResourcesPstmt);
        dataFromResourcesPstmt = null;
        closePStatement(titleFromNotes);
        titleFromNotes = null;
        closePStatement(hashFromResourcesPstmt);
        hashFromResourcesPstmt = null;
        closePStatement(filenameFromResourcesPstmt);
        filenameFromResourcesPstmt = null;
        closePStatement(guidFromNotesPstmt);
        guidFromNotesPstmt = null;
        closePStatement(mimeFromResources);
        mimeFromResources = null;
        closePStatement(ownerguidFromResourcesPstmt);
        ownerguidFromResourcesPstmt = null;
        closePStatement(recogFromResourcesPstmt);
        recogFromResourcesPstmt = null;
        closePStatement(hashResourcesOwnerPstmt);
        hashResourcesOwnerPstmt = null;
        closePStatement(usnFromNotesPstmt);
        usnFromNotesPstmt = null;
    }

    private void closePStatement(PreparedStatement pstmt) {
        if (pstmt != null) {
            try {
                pstmt.close();


            } catch (SQLException e) {
            }
        }
    }

    private Connection getConnection() {
        return Installer.c;


    }

    public synchronized static DbPstmts getInstance() {
        try {
            if (theInstance == null && !closed) {
                theInstance = new DbPstmts();


            }
            return theInstance;


        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);


            return null;


        }
    }

    private Logger getLogger() {
        return LOG;

    }
}
