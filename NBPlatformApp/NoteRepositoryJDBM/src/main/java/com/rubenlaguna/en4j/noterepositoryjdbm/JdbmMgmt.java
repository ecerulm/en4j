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
package com.rubenlaguna.en4j.noterepositoryjdbm;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public class JdbmMgmt implements JdbmMgmtMBean {

    private final int SAMPLES = 50;
    private DescriptiveStatistics getAllNotesDs = new SynchronizedDescriptiveStatistics(SAMPLES);
    private DescriptiveStatistics getByIdDs = new SynchronizedDescriptiveStatistics(SAMPLES);
    private DescriptiveStatistics getByGuiDs = new SynchronizedDescriptiveStatistics(SAMPLES);
    private DescriptiveStatistics insertNoteDs = new SynchronizedDescriptiveStatistics(SAMPLES);
//    private final List<Long> samplesGetById = new LinkedList<Long>();
//    private final List<Long> samplesGetByGuid = new LinkedList<Long>();
//    private final List<Long> samplesGetAllNotes = new LinkedList<Long>();
    private long countGetById = 0;
    private long countGetByGuid = 0;
    private long countGetAllNotes = 0;
    private long countInsertNote;
    private List<Long> samplesInsertNotes = new LinkedList<Long>();

    private long doAvg(List<Long> theList) {
        if (theList.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (Long s : theList) {
            total += s;
        }


        return total / theList.size();
    }

    public JdbmMgmt() {
    }

    @Override
    public double getAvgMsGetById() {
        return (long) getByIdDs.getMean();
//        return doAvg(samplesGetById);
    }

    @Override
    public double getAvgMsGetByGuid() {
        return getByGuiDs.getMean();
//        return doAvg(samplesGetByGuid);
    }

    @Override
    public double getAvgMsGetAllNotes() {
        return getAllNotesDs.getMean();
//        return doAvg(samplesGetAllNotes);
    }
    @Override
    public double getAvgMsInsertNote() {
        return insertNoteDs.getMean();
//        return doAvg(samplesInsertNotes);
    }

    @Override
    public long getNumberOfCallsGetById() {
        return countGetById;
    }

    @Override
    public long getNumberOfCallsGetByGuid() {
        return countGetByGuid;
    }

    @Override
    public long getNumberOfCallsGetAllNotes() {
        return countGetAllNotes;
    }


    @Override
    public long getNumberOfCallsInsertNote() {
        return countInsertNote;
    }
    private void doSample(List<Long> theList, long delta) {
        theList.add(delta);
        while (theList.size() > SAMPLES) {
            theList.remove(0);
        }
    }


    void sampleGetById(long delta) {
        countGetById++;
        getByIdDs.addValue(delta);
//        doSample(samplesGetById, delta);
    }

    void sampleGetAllNotes(long delta) {
        countGetAllNotes++;
        getAllNotesDs.addValue(delta);
//        doSample(samplesGetAllNotes, delta);

    }

    void sampleInsertNote(long delta) {
        countInsertNote++;
        insertNoteDs.addValue(delta);
//        doSample(samplesInsertNotes, l);
    }
}
