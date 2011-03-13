/*
 * Copyright (C) 2011 Ruben Laguna <ruben.laguna@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rubenlaguna.en4j.noterepository;

import com.rubenlaguna.en4j.interfaces.NoteRepository;
import java.util.Collection;
import java.util.logging.Logger;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public abstract class NoteRepositoryChooser {
    
    private static final Logger LOG = Logger.getLogger(NoteRepositoryChooser.class.getName());
    private static NoteRepository theNoteRepository = null;
    
    public static synchronized NoteRepository getDefault() {
        
        if (null != theNoteRepository) {
            return theNoteRepository;
        }
        LOG.info("Creating new NoteRepository");
        Collection<? extends com.rubenlaguna.en4j.interfaces.NoteRepository> all = Lookup.getDefault().lookupAll(com.rubenlaguna.en4j.interfaces.NoteRepository.class);
        if (all.isEmpty()) {
            throw new IllegalStateException("there is no single noterepositoy implementation available");
        }
        for (com.rubenlaguna.en4j.interfaces.NoteRepository r : all) {
            if (r.getName().equals(NbPreferences.forModule(DatabasePanel.class).get(DatabasePanel.PREF_NRIMPL, "-none"))){
                theNoteRepository = r;
                return r;
            }
        }
        com.rubenlaguna.en4j.interfaces.NoteRepository r = all.iterator().next();
        NbPreferences.forModule(DatabasePanel.class).put(DatabasePanel.PREF_NRIMPL, r.getName());
        theNoteRepository = r;
        return theNoteRepository;
    }

}
