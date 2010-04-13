/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.sync;

import java.util.Collection;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
interface EDAMIf {

    Collection<NoteInfo> getSyncChunk(int highestUSN, int numnotes, boolean isFirstSync);

    com.rubenlaguna.en4j.noteinterface.NoteReader getNote(String noteGuid, boolean b, boolean b0, boolean b1, boolean b2) throws Exception;

    int getUpdateCount();

    public boolean checkVersion();
}
