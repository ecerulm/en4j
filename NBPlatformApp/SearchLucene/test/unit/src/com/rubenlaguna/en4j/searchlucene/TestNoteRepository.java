/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.noteinterface.Note;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.progress.ProgressHandle;

/**
 *
 * @author ecerulm
 */
class TestNoteRepository implements NoteRepository {

    private final Map<Integer, Note> theMap = new HashMap<Integer, Note>();

    public TestNoteRepository() {
    }

    public Collection<Note> getAllNotes() {
        System.out.println("getAllNotes XXXXX");
        System.out.println("this=" + this);
        return theMap.values();
    }

    public void importEntries(InputStream in, ProgressHandle ph) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Note get(int id) {
        return theMap.get(id);
    }

    public boolean add(Note note) {
        theMap.put(note.getId(), note);
        System.out.println("size " + theMap.size());

        System.out.println("this=" + this);
        return true;
    }

    public int getHighestUSN() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Note get(int id, boolean withContents) {
        return get(id);
    }

    public Note getByGuid(String guid, boolean withContents) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
