/*
 * NoteRepositoryH2.java
 *
 * Created on April 11, 2010, 6:44 PM
 */
package com.rubenlaguna.en4j.noterepository;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics;

/**
 * Class NoteRepositoryH2
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public class NoteRepositoryH2Data implements NoteRepositoryH2DataMBean {

    private DescriptiveStatistics getAllNotesDs = new SynchronizedDescriptiveStatistics(10);
    private DescriptiveStatistics getNoteDs = new SynchronizedDescriptiveStatistics(10);

    public NoteRepositoryH2Data() {
    }

    /**
     * Get Number of ms that takes to retrieve all notes from database
     */
    public double getGetAllNotesAverageMs() {
        return getAllNotesDs.getMean();
    }

    public void sampleGetAllNotes(long i) {
        getAllNotesDs.addValue(i);
    }

    /**
     * Get mean time to get a note
     */
    public double getGetNoteAverage() {
        return getNoteDs.getMean();
    }

    public void sampleGetNote(long i) {
        getNoteDs.addValue(i);
    }
}


