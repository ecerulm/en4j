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

package com.rubenlaguna.en4j.noterepository;

/**
 * Interface NoteRepositoryH2MBean
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public interface NoteRepositoryH2DataMBean
{

    /**
     * Get Number of ms that takes to retrieve all notes from database
     */
    public double getGetAllNotesAverageMs();

    /**
     * Get mean time to get a note
     */
    public double getGetNoteAverage();


    
}


