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
package com.rubenlaguna.en4j.sync;

import com.evernote.edam.type.Note;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author ecerulm
 */
class NoteAdapter implements com.rubenlaguna.en4j.noteinterface.Note {

    private final Note adaptee;
    private final Logger LOG = Logger.getLogger(NoteAdapter.class.getName());

    public NoteAdapter(Note note) {
        this.adaptee = note;
    }

    public String getContent() {
//        LOG.info("adaptee ="+adaptee);
//        LOG.info("adaptee.getContent() ="+adaptee.getContent());
        return adaptee.getContent();
    }

    public void setContent(String content) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getCreated() {
        return new Date(adaptee.getCreated() * 1000L);
    }

    public void setCreated(Date created) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Integer getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setId(Integer id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getSourceurl() {
        return adaptee.getAttributes().getSourceURL();
    }

    public void setSourceurl(String sourceurl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getTitle() {
//        LOG.info("adaptee.getTitle = \""+adaptee.getTitle()+"\"");
        return adaptee.getTitle();
    }

    public void setTitle(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getUpdated() {
        return new Date(adaptee.getUpdated() * 1000L);
    }

    public void setUpdated(Date updated) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getUpdateSequenceNumber() {
        return adaptee.getUpdateSequenceNum();
    }

    public Resource getResource(String hash) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Resource> getResources() {
        final List<Resource> toReturn = new ArrayList<Resource>();
        final Iterator<com.evernote.edam.type.Resource> resourcesIterator = adaptee.getResourcesIterator();
        if (null != resourcesIterator) {
            while (resourcesIterator.hasNext()) {
                final com.evernote.edam.type.Resource resource = resourcesIterator.next();

                Resource resourceToReturn = new Resource() {

                    public byte[] getData() {
                        return resource.getData().getBody();
                    }
                };
                toReturn.add(resourceToReturn);
            }
        }
        return toReturn;
    }

    public String getGuid() {
        return adaptee.getGuid();
    }
}
