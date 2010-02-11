/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
        Resource toReturn = new ResourceAdapter(adaptee.getResource(hash));
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
}
