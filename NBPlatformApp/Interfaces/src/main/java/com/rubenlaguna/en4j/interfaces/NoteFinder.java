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
import java.util.Collection;
import org.netbeans.api.progress.ProgressHandle;

/**
 *
 * @author ecerulm
 */
public interface NoteFinder {

    Collection<Note> find(String search);

    void index(Note n);

    void remove(Note n);

    void removeByGuid(String noteguid);

    void rebuildIndex(ProgressHandle ph);

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    void close();
}
