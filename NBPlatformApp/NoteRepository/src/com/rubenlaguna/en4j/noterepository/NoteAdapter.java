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

import com.rubenlaguna.en4j.jpaentities.Notes;
import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.rmi.UnexpectedException;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

/**
 *
 * @author ecerulm
 */
class NoteAdapter implements Note {

    private final Notes adaptee;

    public String toString() {
        return adaptee.toString();
    }

    public void setUpdated(Date updated) {
        adaptee.setUpdated(updated);
    }

    public void setTitle(String title) {
        adaptee.setTitle(title);
    }

    public void setSourceurl(String sourceurl) {
        adaptee.setSourceurl(sourceurl);
    }

    public void setId(Integer id) {
        adaptee.setId(id);
    }

    public void setCreated(Date created) {
        adaptee.setCreated(created);
    }

    public void setContent(String content) {
        adaptee.setContent(content);
    }

    public int hashCode() {
        return adaptee.hashCode();
    }

    public Date getUpdated() {
        return adaptee.getUpdated();
    }

    public String getTitle() {
        return adaptee.getTitle();
    }

    public String getSourceurl() {
        return adaptee.getSourceurl();
    }

    public Resource getResource(String hash) {
        final com.rubenlaguna.en4j.jpaentities.Resource resource = adaptee.getResource(hash);
        if (null == resource) {
            return null;
        }
        Resource toReturn = new ResourceAdapter(resource);
        return toReturn;
    }

    public Integer getId() {
        return adaptee.getId();
    }

    public Date getCreated() {
        return adaptee.getCreated();
    }

    public String getContent() {
        return adaptee.getContent();
    }

    public boolean equals(Object object) {
        return adaptee.equals(object);
    }

    public void addResource(Resource resource) {
        throw new UnsupportedOperationException();

        //adaptee.addResource(resource);
    }

    public NoteAdapter(Notes origNotes) {
        this.adaptee = origNotes;
    }

    public Collection<Resource> getResources() {
        final Collection<Resource> toReturn = CollectionUtils.collect(this.adaptee.getResources(), new Transformer() {

            public Object transform(Object o) {
                return new ResourceAdapter((com.rubenlaguna.en4j.jpaentities.Resource) o);
            }
        });
        return toReturn;
    }

    public int getUpdateSequenceNumber() {
        return adaptee.getUpdateSequenceNumber();
    }

    public String getGuid() {
        return adaptee.getGuid();
    }

    public boolean isActive() {
        return adaptee.isActive();
    }

    public void setActive(boolean active) {
        adaptee.setActive(active);
    }

    public Date getDeleted() {
        return adaptee.getDeleted();
    }

    public void setDeleted(Date deleted) {
        adaptee.setDeleted(deleted);
    }
}
