/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 *
 * @author ecerulm
 */
class AbstractNote implements Note {

    private int id = 0;
    private String content = "";
    private String title = "";

    public AbstractNote() {
    }

    public AbstractNote(int id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public String getContent() {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF - 8\"?>");
        sb.append("<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml.dtd\">");
        sb.append("        <en-note>");
        sb.append(content);
        sb.append("</en-note>");
        return sb.toString();
    }

    public void setContent(String content) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getCreated() {
        throw new UnsupportedOperationException("Not supported yet.");
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
        return "";
    }

    public void setSourceurl(String sourceurl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Date getUpdated() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setUpdated(Date updated) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Resource getResource(String hash) {
        throw new UnsupportedOperationException("Not supported yet." + hash);
    }

    public Collection<Resource> getResources() {
        return Collections.EMPTY_LIST;
    }

    public int getUpdateSequenceNumber() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getGuid() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
