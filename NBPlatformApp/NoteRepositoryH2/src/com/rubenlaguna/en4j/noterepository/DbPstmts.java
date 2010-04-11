/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    PreparedStatementWrapperInt sourceurlPstmt;
    PreparedStatementWrapperInt contentFromNotes;
    PreparedStatementWrapperString dataFromResourcesPstmt;
    PreparedStatementWrapperInt guidFromNotesPstmt;
    PreparedStatementWrapperString recogFromResourcesPstmt;
    PreparedStatementWrapperString ownerguidFromResourcesPstmt;
    PreparedStatementWrapperString mimeFromResources;
    PreparedStatementWrapperString hashResourcesOwnerPstmt;
    PreparedStatementWrapperInt usnFromNotesPstmt;
    PreparedStatementWrapperString filenameFromResourcesPstmt;
    PreparedStatementWrapperString hashFromResourcesPstmt;
    PreparedStatementWrapperInt titleFromNotes;

    private DbPstmts() throws SQLException {
        contentFromNotes = new PreparedStatementWrapperInt(getConnection().prepareStatement("SELECT CONTENT FROM NOTES WHERE ID=?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
                final Reader characterStream = rs.getCharacterStream("CONTENT");
                return characterStream;
            }
        };

        sourceurlPstmt = new PreparedStatementWrapperInt(getConnection().prepareStatement("SELECT SOURCEURL FROM NOTES WHERE ID =?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
                final String toReturn = rs.getString("SOURCEURL");
                return toReturn;
            }
        };
        titleFromNotes = new PreparedStatementWrapperInt(getConnection().prepareStatement("SELECT TITLE FROM NOTES WHERE ID =?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getString("TITLE");
            }
        };

        usnFromNotesPstmt = new PreparedStatementWrapperInt(getConnection().prepareStatement("SELECT USN FROM NOTES WHERE ID =?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getInt("USN");
            }
        };
        hashResourcesOwnerPstmt = new PreparedStatementWrapperString(getConnection().prepareStatement("SELECT HASH FROM RESOURCES WHERE OWNERGUID =?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
                Collection<String> toReturn = new ArrayList<String>();
                do {
                    toReturn.add(rs.getString("HASH"));
                } while (rs.next());
                return toReturn;
            }
        };
        guidFromNotesPstmt = new PreparedStatementWrapperInt(getConnection().prepareStatement("SELECT GUID FROM NOTES WHERE ID =?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getString("GUID");
            }
        };
        dataFromResourcesPstmt = new PreparedStatementWrapperString(getConnection().prepareStatement("SELECT DATA FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
                final Blob blob = rs.getBlob("DATA");
                final InputStream is = blob.getBinaryStream();
                return is;
            }
        };
        mimeFromResources = new PreparedStatementWrapperString(getConnection().prepareStatement("SELECT MIME FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getString("MIME");
            }
        };

        ownerguidFromResourcesPstmt = new PreparedStatementWrapperString(getConnection().prepareStatement("SELECT OWNERGUID FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getString("OWNERGUID");
            }
        };
        recogFromResourcesPstmt = new PreparedStatementWrapperString(getConnection().prepareStatement("SELECT RECOGNITION FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
            return rs.getBytes("RECOGNITION");
            }
        };
        filenameFromResourcesPstmt = new PreparedStatementWrapperString(getConnection().prepareStatement("SELECT FILENAME FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected Object getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getString("FILENAME");
            }
        };
    }

    public Reader getContentAsReader(int id) {
        return (Reader) contentFromNotes.get(id);
    }

    public String getSourceurl(int id) {
        return (String) sourceurlPstmt.get(id);
    }

    public String getTitle(int id) {
        return (String) titleFromNotes.get(id);
    }

    public int getUpdateSequenceNumber(int id) {
        return (Integer) usnFromNotesPstmt.get(id);
    }

    public Collection<String> getResources(String guid) {
        final Collection<String> resources = (Collection<String>) hashResourcesOwnerPstmt.get(guid);
        if(resources==null) {
            return Collections.EMPTY_LIST;
        }
        return resources;
    }

    public String getGuid(int id) {
        return (String) guidFromNotesPstmt.get(id);
    }

    public String getDataHash(String resGuid) {
        return (String) hashFromResourcesPstmt.get(resGuid);
    }

    public InputStream getDataAsInputStream(String guid) {
        return (InputStream) dataFromResourcesPstmt.get(guid);
    }

    public byte[] getData(String resGuid) {
        try {
            BufferedInputStream is = new BufferedInputStream(getDataAsInputStream(resGuid));
            ReadableByteChannel isc = Channels.newChannel(is);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            WritableByteChannel osc = Channels.newChannel(os);
            ByteBuffer bb = ByteBuffer.allocate(32000);
            while (isc.read(bb) != -1) {
                bb.flip();
                osc.write(bb);
                bb.clear();
            }
            return os.toByteArray();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    public String getFilename(String resGuid) {
        return (String) filenameFromResourcesPstmt.get(resGuid);
    }

    public String getMime(String guid) {
        return (String) mimeFromResources.get(guid);
    }

    public String getNoteguid(String resGuid) {
        return (String) ownerguidFromResourcesPstmt.get(resGuid);
    }

    public byte[] getRecognition(String resGuid) {
        return (byte[]) recogFromResourcesPstmt.get(resGuid);
    }

    public synchronized void close() {
        closed = true;
        theInstance = null;
//        closePStatement(contentFromNotesPstmt);
        contentFromNotes.close();
        contentFromNotes = null;

        sourceurlPstmt.close();
        sourceurlPstmt = null;

        dataFromResourcesPstmt.close();
        dataFromResourcesPstmt = null;

        titleFromNotes.close();
        titleFromNotes = null;

        hashFromResourcesPstmt.close();
        hashFromResourcesPstmt = null;

        filenameFromResourcesPstmt.close();
        filenameFromResourcesPstmt = null;

        guidFromNotesPstmt.close();
        guidFromNotesPstmt = null;

        mimeFromResources.close();
        mimeFromResources = null;

        ownerguidFromResourcesPstmt.close();
        ownerguidFromResourcesPstmt = null;

        recogFromResourcesPstmt.close();
        recogFromResourcesPstmt = null;

        hashResourcesOwnerPstmt.close();
        hashResourcesOwnerPstmt = null;

        usnFromNotesPstmt.close();
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
