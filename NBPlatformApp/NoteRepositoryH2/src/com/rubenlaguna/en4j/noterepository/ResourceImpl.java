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

    public int getUpdateSequenceNumber() {
        return DbPstmts.getInstance().getUpdateSequenceNumberForResource(guid);
    }
}
