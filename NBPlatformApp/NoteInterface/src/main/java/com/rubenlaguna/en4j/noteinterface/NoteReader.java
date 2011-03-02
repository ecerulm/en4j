/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noteinterface;

import java.io.Reader;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public interface NoteReader {

    String getContent();
    Reader getContentAsReader();

    Date getCreated();

    String getSourceurl();

    Date getUpdated();

    String getTitle();

    Resource getResource(String hash);

    Collection<Resource> getResources();

    String getGuid();

    Date getDeleted();

    boolean isActive();

    int getUpdateSequenceNumber();
}
