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
package com.rubenlaguna.en4j.mainmodule;

import ca.odell.glazedlists.matchers.Matcher;
import com.rubenlaguna.en4j.noteinterface.Note;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class NotesMatcher implements Matcher<Note> {

    private final Collection<Note> internalList = new ArrayList<Note>();
    private boolean goThrough = false;

    public NotesMatcher(Collection<Note> notesToBeShown) {
        if (notesToBeShown == null) {
            goThrough = true;
        } else {
            internalList.addAll(notesToBeShown); //deep copy list
        }
    }

    @Override
    public boolean matches(Note item) {
        if (goThrough) {
            return true;
        }
        return internalList.contains(item);
    }
}
