/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rubenlaguna.en4j.interfaces;

import com.rubenlaguna.en4j.jpaentities.Notes;
import java.util.Collection;

/**
 *
 * @author ecerulm
 */
public interface NoteRepository {
    Collection<Notes> getAllNotes();
    //TODO add a rebuildIndex method
    public Notes get(int id);
    
}
