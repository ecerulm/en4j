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

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import com.rubenlaguna.en4j.noteinterface.Note;
import java.util.Collection;

/**
 * This {@link MatcherEditor} matches notes if they are in the list
 *
 * @author <a href="mailto:ruben.laguna@gmail.com">Ruben Laguna</a>
 */
public class NoteMatcherEditor extends AbstractMatcherEditor<Note>  {

    public void refilter(Collection<Note> notesToBeShown) {
        Matcher<Note> newMatcher = new NotesMatcher(notesToBeShown);
        fireChanged(newMatcher);
    }

}
