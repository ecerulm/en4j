/*
 * NoteRepositoryH2MBean.java
 *
 * Created on April 11, 2010, 6:44 PM
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


