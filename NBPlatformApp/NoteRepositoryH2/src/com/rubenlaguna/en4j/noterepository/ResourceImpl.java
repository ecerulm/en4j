/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Date;
import java.util.logging.Logger;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class ResourceImpl implements Resource, Serializable {

    private final String guid;

    public ResourceImpl(String guid) {
        this.guid = guid;
    }

    public byte[] getAlternateData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getAltitude() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getCameraMake() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getCameraModel() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte[] getData() {
        return DbPstmts.getInstance().getData(guid);
    }

    public InputStream getDataAsInputStream() {
        return DbPstmts.getInstance().getDataAsInputStream(guid);
    }

    public String getDataHash() {
        return DbPstmts.getInstance().getDataHash(guid);
    }

    public String getAlternateDataHash() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getFilename() {
        return DbPstmts.getInstance().getFilename(guid);
    }

    public String getGuid() {
        return guid;
    }

    public double getLatitude() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getLongitude() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getMime() {
        return DbPstmts.getInstance().getMime(guid);
    }

    public String getNoteguid() {
        return DbPstmts.getInstance().getNoteguid(guid);
    }

    public boolean getPremiumAttachment() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte[] getRecognition() {
        return DbPstmts.getInstance().getRecognition(guid);
    }

    public Date getTimestamp() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private Connection getConnection() {
        return Installer.c;
    }

    private Logger getLogger() {
        return Logger.getLogger(ResourceImpl.class.getName());
    }
}
