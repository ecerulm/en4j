/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class ResourceImpl implements Resource, Serializable {

//    private  transient Logger LOG = Logger.getLogger(ResourceImpl.class.getName());
    private final byte[] alternateData;
    private final double altitude;
    private final String cameraMake;
    private final String cameraModel;
//    private final byte[] data;
    private final String datahash;
    private final double latitude;
    private final double longitude;
    private final String filename;
    private final String guid;
    private final String mime;
    private final String noteguid;
    private final boolean premiumattachment;
    private final byte[] recognition;
    private final Date timestamp;
    private final String alternateDataHash;

    public ResourceImpl(Resource resource) {
        this.alternateData = resource.getAlternateData();
        this.alternateDataHash = resource.getAlternateDataHash();
        this.altitude = resource.getAltitude();
        this.cameraMake = resource.getCameraMake();
        this.cameraModel = resource.getCameraModel();
//        this.data = resource.getData();
        this.datahash = resource.getDataHash();
        this.filename = resource.getFilename();
        this.guid = resource.getGuid();
        this.latitude = resource.getLatitude();
        this.longitude = resource.getLongitude();
        this.mime = resource.getMime();
        this.noteguid = resource.getNoteguid();
        this.premiumattachment = resource.getPremiumAttachment();
        this.recognition = resource.getRecognition();
        this.timestamp = resource.getTimestamp();
    }

    public byte[] getAlternateData() {
        return alternateData;
    }

    public double getAltitude() {
        return altitude;
    }

    public String getCameraMake() {
        return cameraMake;
    }

    public String getCameraModel() {
        return cameraModel;
    }

    public byte[] getData() {
        getLogger().info("resource guid:" + guid);
        try {
            PreparedStatement pstmt = getConnection().prepareStatement("SELECT DATA FROM RESOURCES WHERE GUID=?");
            pstmt.setString(1, guid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                final Blob blob = rs.getBlob("DATA");
                final InputStream is = blob.getBinaryStream();
                // Create the byte array to hold the data
                final byte[] toReturn = new byte[(int) blob.length()];

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
                        getLogger().warning("could not completely read data for resource guid:"+guid);
                        //throw new IOException("Could not completely read file " + file.getName());
                    }
                } catch (IOException e) {
                    getLogger().log(Level.WARNING, "caught exception:", e);
                } finally {
                    // Close the input stream and return bytes
                    try {is.close();} catch (IOException e) {}
                }
                return toReturn;
            }

        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
        }

        return null;
    }
    public InputStream getDataAsInputStream() {
        getLogger().info("resource guid:" + guid);
        try {
            PreparedStatement pstmt = getConnection().prepareStatement("SELECT DATA FROM RESOURCES WHERE GUID=?");
            pstmt.setString(1, guid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                final Blob blob = rs.getBlob("DATA");
                final InputStream is = blob.getBinaryStream();
                return is;
            }

        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
        }

        return null;
    }

    public String getDataHash() {
        return datahash;
    }

    public String getAlternateDataHash() {
        return alternateDataHash;
    }

    public String getFilename() {
        return filename;
    }

    public String getGuid() {
        return guid;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getMime() {
        return mime;
    }

    public String getNoteguid() {
        return noteguid;
    }

    public boolean getPremiumAttachment() {
        return premiumattachment;
    }

    public byte[] getRecognition() {
        return recognition;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    private Connection getConnection() {
        return Installer.c;
    }

    private Logger getLogger() {
        return Logger.getLogger(ResourceImpl.class.getName());
    }
}
