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

import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Lookup;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class NoteImpl implements Note, Comparable {

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
        StringBuilder sb = new StringBuilder(64000);
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
        return DbPstmts.getInstance().getContentAsReader(id);
    }

    public void setContent(String content) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getCreated() {
        return DbPstmts.getInstance().getCreated(id);
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
        return DbPstmts.getInstance().getSourceurl(id);
    }

    public void setSourceurl(String sourceurl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getTitle() {
        return DbPstmts.getInstance().getTitle(id);
    }

    public void setTitle(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getUpdated() {
        return DbPstmts.getInstance().getUpdated(id);
    }

    public void setUpdated(Date updated) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getUpdateSequenceNumber() {
        return DbPstmts.getInstance().getUpdateSequenceNumber(id);
    }

    public boolean isActive() {
        return DbPstmts.getInstance().isActive(id);
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
        final String guid = getGuid();
        if (null == guid) {
            return null;
        }
        if (hash.length() < 32) {
            getLogger().warning("Hash should be 32 char long. Left padding it with zeros");
            StringBuffer sb = new StringBuffer();
            for (int i = hash.length(); i < 32; i++) {
                sb.append("0");
            }
            sb.append(hash);
            final String paddedHash = sb.toString();
            getLogger().warning("changed from " + hash + " (" + hash.length() + ") to " + paddedHash + " (" + paddedHash.length() + ")");
            getResource(paddedHash);
        }
        if (hash.length() != 32) {
            throw new IllegalArgumentException("hash has to be 32 bytes long. this " + hash + " was " + hash.length());
        }
        return Lookup.getDefault().lookup(NoteRepositoryH2Impl.class).getResource(guid, hash);
    }

    @Override
    public Collection<Resource> getResources() {
        Set<Resource> toReturn = new HashSet<Resource>();
        final String guid = getGuid();
        if (null == guid) {
            return Collections.EMPTY_LIST;
        }
        Collection<String> resHashes = DbPstmts.getInstance().getResources(guid);
        for (String hash : resHashes) {
            toReturn.add(getResource(hash));
        }
        return toReturn;
    }

    public String getGuid() {
        return DbPstmts.getInstance().getGuid(id);
    }

    private Connection getConnection() {
        return Installer.c;
    }

    private Logger getLogger() {
        return Logger.getLogger(NoteImpl.class.getName());
    }

    public int compareTo(Object o) {
        if (o instanceof NoteImpl) {
            NoteImpl v = (NoteImpl) o;
            final Date createdrhs = getCreated();
            if (null != createdrhs) {
                final Date createdlhs = v.getCreated();
                if (null != createdlhs) {
                    return -(createdrhs.compareTo(createdlhs));
                }
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NoteImpl other = (NoteImpl) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.id;
        return hash;
    }
    
}
