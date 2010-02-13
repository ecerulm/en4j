/*
 *  Copyright (C) 2010 ecerulm
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
package com.rubenlaguna.en4j.interfaces;

import com.rubenlaguna.en4j.noteinterface.Note;
import java.beans.PropertyChangeListener;
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

    void importEntries(InputStream in, ProgressHandle ph) throws InterruptedException;

    Note get(int id);

    Note get(int id, boolean withContents);

    Note getByGuid(String guid, boolean withContents);

    boolean add(Note n);

    int getHighestUSN();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
