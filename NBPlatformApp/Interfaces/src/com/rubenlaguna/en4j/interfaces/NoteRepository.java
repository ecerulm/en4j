/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.interfaces;

import com.rubenlaguna.en4j.noteinterface.Note;
import java.io.InputStream;
import java.util.Collection;
import org.netbeans.api.progress.ProgressHandle;

/**
 *
 * @author ecerulm
 */
public interface NoteRepository {

    Collection<Note> getAllNotes();
    //TODO add a rebuildIndex method

    void importEntries(InputStream in,ProgressHandle ph);
    Note get(int id);


    
}
