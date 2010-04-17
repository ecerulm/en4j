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
    PreparedStatementWrapper<Integer, String> sourceurlPstmt;
    PreparedStatementWrapper<Integer, Reader> contentFromNotes;
    PreparedStatementWrapper<String, InputStream> dataFromResourcesPstmt;
    PreparedStatementWrapper<Integer, String> guidFromNotesPstmt;
    PreparedStatementWrapper<String, byte[]> recogFromResourcesPstmt;
    PreparedStatementWrapper<String, Integer> usnFromResourcesGuidPstmt;
    PreparedStatementWrapper<String, String> ownerguidFromResourcesPstmt;
    PreparedStatementWrapper<String, String> mimeFromResources;
    PreparedStatementWrapper<String, Collection<String>> hashResourcesOwnerPstmt;
    PreparedStatementWrapper<Integer, Integer> usnFromNotesPstmt;
    PreparedStatementWrapper<String, String> filenameFromResourcesPstmt;
    PreparedStatementWrapper<String, String> hashFromResourcesPstmt;
    PreparedStatementWrapper<Integer, String> titleFromNotes;
    PreparedStatementWrapper<Integer, Boolean> isNoteActiveFromId;

    private DbPstmts() throws SQLException {
        contentFromNotes = new PreparedStatementWrapper<Integer, Reader>(getConnection().prepareStatement("SELECT CONTENT FROM NOTES WHERE ID=?")) {

            @Override
            protected Reader getResultFromResulSet(ResultSet rs) throws SQLException {
                final Reader characterStream = rs.getCharacterStream("CONTENT");
                return characterStream;
            }
        };

        sourceurlPstmt = new PreparedStatementWrapper<Integer, String>(getConnection().prepareStatement("SELECT SOURCEURL FROM NOTES WHERE ID =?")) {

            @Override
            protected String getResultFromResulSet(ResultSet rs) throws SQLException {
                final String toReturn = rs.getString("SOURCEURL");
                return toReturn;
            }
        };
        titleFromNotes = new PreparedStatementWrapper<Integer, String>(getConnection().prepareStatement("SELECT TITLE FROM NOTES WHERE ID =?")) {

            @Override
            protected String getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getString("TITLE");
            }
        };

        usnFromNotesPstmt = new PreparedStatementWrapper<Integer, Integer>(getConnection().prepareStatement("SELECT USN FROM NOTES WHERE ID =?")) {

            @Override
            protected Integer getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getInt("USN");
            }
        };
        hashResourcesOwnerPstmt = new PreparedStatementWrapper<String, Collection<String>>(getConnection().prepareStatement("SELECT HASH FROM RESOURCES WHERE OWNERGUID =?")) {

            @Override
            protected Collection<String> getResultFromResulSet(ResultSet rs) throws SQLException {
                Collection<String> toReturn = new ArrayList<String>();
                do {
                    toReturn.add(rs.getString("HASH"));
                } while (rs.next());
                return toReturn;
            }
        };
        guidFromNotesPstmt = new PreparedStatementWrapper<Integer, String>(getConnection().prepareStatement("SELECT GUID FROM NOTES WHERE ID =?")) {

            @Override
            protected String getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getString("GUID");
            }
        };
        dataFromResourcesPstmt = new PreparedStatementWrapper<String, InputStream>(getConnection().prepareStatement("SELECT DATA FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected InputStream getResultFromResulSet(ResultSet rs) throws SQLException {
                final Blob blob = rs.getBlob("DATA");
                final InputStream is = blob.getBinaryStream();
                return is;
            }
        };
        mimeFromResources = new PreparedStatementWrapper<String, String>(getConnection().prepareStatement("SELECT MIME FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected String getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getString("MIME");
            }
        };

        ownerguidFromResourcesPstmt = new PreparedStatementWrapper<String, String>(getConnection().prepareStatement("SELECT OWNERGUID FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected String getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getString("OWNERGUID");
            }
        };
        recogFromResourcesPstmt = new PreparedStatementWrapper<String, byte[]>(getConnection().prepareStatement("SELECT RECOGNITION FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected byte[] getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getBytes("RECOGNITION");
            }
        };
        usnFromResourcesGuidPstmt = new PreparedStatementWrapper<String, Integer>(getConnection().prepareStatement("SELECT USN FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected Integer getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getInt("USN");
            }
        };
        filenameFromResourcesPstmt = new PreparedStatementWrapper<String, String>(getConnection().prepareStatement("SELECT FILENAME FROM RESOURCES WHERE GUID=?")) {

            @Override
            protected String getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getString("FILENAME");
            }
        };
        isNoteActiveFromId = new PreparedStatementWrapper<Integer, Boolean>(getConnection().prepareStatement("SELECT ISACTIVE FROM NOTES WHERE ID=?")) {

            @Override
            protected Boolean getResultFromResulSet(ResultSet rs) throws SQLException {
                return rs.getBoolean("ISACTIVE");
            }
        };
    }

    public Reader getContentAsReader(int id) {
        return contentFromNotes.get(id);
    }

    public String getSourceurl(int id) {
        return sourceurlPstmt.get(id);
    }

    public String getTitle(int id) {
        return titleFromNotes.get(id);
    }

    public int getUpdateSequenceNumber(int id) {
        return usnFromNotesPstmt.get(id);
    }

    public Collection<String> getResources(String guid) {
        final Collection<String> resources = hashResourcesOwnerPstmt.get(guid);
        if (resources == null) {
            return Collections.EMPTY_LIST;
        }
        return resources;
    }

    public String getGuid(int id) {
        return guidFromNotesPstmt.get(id);
    }

    public String getDataHash(String resGuid) {
        return hashFromResourcesPstmt.get(resGuid);
    }

    public InputStream getDataAsInputStream(String guid) {
        return dataFromResourcesPstmt.get(guid);
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
        return filenameFromResourcesPstmt.get(resGuid);
    }

    public String getMime(String guid) {
        return mimeFromResources.get(guid);
    }

    public String getNoteguid(String resGuid) {
        return ownerguidFromResourcesPstmt.get(resGuid);
    }

    public byte[] getRecognition(String resGuid) {
        return recogFromResourcesPstmt.get(resGuid);
    }

    int getUpdateSequenceNumberForResource(String resGuid) {
        return usnFromResourcesGuidPstmt.get(resGuid);
    }

    boolean isActive(int id) {
        return isNoteActiveFromId.get(id);
    }

    public synchronized void close() {
        closed = true;
        theInstance = null;

        if (!closed) {
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
