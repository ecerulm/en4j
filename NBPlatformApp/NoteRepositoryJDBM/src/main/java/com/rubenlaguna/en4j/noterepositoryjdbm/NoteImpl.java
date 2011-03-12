/*
 * Copyright (C) 2011 Ruben Laguna <ruben.laguna@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.rubenlaguna.en4j.noterepositoryjdbm;

import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.NoteReader;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.openide.util.Lookup;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class NoteImpl implements Note, Serializable, Comparable {

    static final long serialVersionUID = 2L;
    private final String content;
    private final Date created;
    private final Date deleted;
    private final String guid;
    private final String sourceUrl;
    private final String title;
    private final int usn;
    private final Date updated;
    private final Collection<String> resourceHashes;
    private final boolean isActive;
    private final int id;

    NoteImpl(NoteReader note, int id) {
        content = note.getContent();
        created = note.getCreated();
        deleted = note.getDeleted();
        guid = note.getGuid();
        sourceUrl = note.getSourceurl();
        title = note.getTitle();
        isActive = note.isActive();
        usn = note.getUpdateSequenceNumber();
        updated = note.getUpdated();
        resourceHashes = CollectionUtils.collect(note.getResources(), new Transformer() {

            @Override
            public Object transform(Object input) {
                return ((Resource) input).getDataHash();
            }
        });
        this.id = id;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public Reader getContentAsReader() {
        return new StringReader(content);
    }

    @Override
    public Date getCreated() {
        return (Date) created.clone();
    }

    @Override
    public String getSourceurl() {
        return sourceUrl;
    }

    @Override
    public Date getUpdated() {
        return (Date) updated.clone();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Resource getResource(String hash) {
        return Lookup.getDefault().lookup(NoteRepositoryJDBMImpl.class).getResource(guid, hash);
    }

    @Override
    public Collection<Resource> getResources() {
        return CollectionUtils.collect(resourceHashes, new Transformer() {

            @Override
            public Object transform(Object hash) {
                return getResource((String) hash);
            }
        });
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public Date getDeleted() {
        return (Date) deleted.clone();
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public int getUpdateSequenceNumber() {
        return usn;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setContent(String content) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCreated(Date created) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setId(Integer id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSourceurl(String sourceurl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setTitle(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setUpdated(Date updated) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setActive(boolean active) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDeleted(Date deleted) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
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
