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
package com.rubenlaguna.en4j.sync;

import com.evernote.edam.notestore.SyncChunk;
import com.evernote.edam.type.Resource;
import com.rubenlaguna.en4j.interfaces.NoteFinder;
import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.interfaces.SynchronizationService;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 *
 * @author ecerulm
 */
public class SynchronizationServiceImpl implements SynchronizationService {

    public static final String HIGHESTUSN = "highestUSN";
    private static final String LASTSYNC = "lastsync";
    private final Logger LOG = Logger.getLogger(SynchronizationServiceImpl.class.getName());
    private int PendingRemoteUpdateNotes = 0;
    private static final int MAX_QUEUED_NOTES = 25;
    private final ThreadPoolExecutor RP = new ThreadPoolExecutor(2, 2, 10, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(MAX_QUEUED_NOTES * 2), new ThreadPoolExecutor.CallerRunsPolicy());
    protected boolean syncFailed = false;
    public static final String PROP_SYNCFAILED = "syncFailed";
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private final EvernoteProtocolUtil util;
    private final Semaphore sem = new Semaphore(1);

    public SynchronizationServiceImpl() {
        com.rubenlaguna.en4j.sync.Installer.mbean.setThreadPoolExecutor(RP);
        final String fakeEDAM = System.getProperty("fakeEDAM");

        System.out.println("fakeEDAM =" + fakeEDAM);

        if (fakeEDAM != null) {
            throw new UnsupportedOperationException("no fakeEDAM");
        } else {
            util = EvernoteProtocolUtil.getInstance();
        }
    }

    @Override
    public boolean sync() { // Set up the UserStore and check that we can talk to the server
        boolean success = sem.tryAcquire();
        if (!success) {
            LOG.info("There is another sync running.");
            return false;
        }
        try {
            setPendingRemoteUpdateNotes(-1);
            setSyncFailed(false);
            boolean versionOk = util.checkVersion();
            if (!versionOk) {
                LOG.warning("Incompatible EDAM client protocol version");
                setSyncFailed(true);
                return false;
            }

//            final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
            boolean errorDetected = false;
            boolean moreNotesToDownload = true;
            int initialPendingUpdates = -1;
            do {
                int highestUSN = getHighestUSN();
                LOG.info("highest updateSequenceNumber in the database = " + highestUSN);
                //boolean fullSync = (highestUSN == 0);
                LOG.info("retrieving SyncChunk");
                boolean isFirstSync = isFirstSync();
                if (isFirstSync) {
                    LOG.info("This is still the first sync.");
                }
                final SyncChunk syncChunk = util.getSyncChunk(highestUSN, MAX_QUEUED_NOTES, isFirstSync);
                int pendingUpdates = util.getUpdateCount() - highestUSN;
                if (initialPendingUpdates == -1) {
                    initialPendingUpdates=pendingUpdates;
                }
                final int percentage = (int) ((1.0 - ((float) pendingUpdates / initialPendingUpdates))*100);
                StatusDisplayer.getDefault().setStatusText("Downloading notes ("+percentage+" %)");

                setPendingRemoteUpdateNotes(pendingUpdates);

                if (syncChunk.getChunkHighUSN() > highestUSN) {
                    final List<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();
                    tasks.addAll(createAndSubmitAddNoteTasks(syncChunk));
                    tasks.addAll(createAndSubmitAddResourceTasks(syncChunk));
                    tasks.addAll(createAndSubmitAddExpungedNoteTasks(syncChunk));

                    if (!waitAndUpdateUsn(tasks, syncChunk.getChunkHighUSN())) {
                        return false;
                    }
                } else {
                    LOG.info("No notes to download");
                    moreNotesToDownload = false;
                    setLastSync(System.currentTimeMillis());
                }
            } while (!errorDetected && moreNotesToDownload);
            return !errorDetected;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Sync couldn't complete because of", ex);
            return false;
        } finally {
            sem.release();
        }
    }

    public List<Future<Boolean>> createAndSubmitAddResourceTasks(final SyncChunk syncChunk) {
        final List<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();
        if (syncChunk.isSetResources()) {
            int husn = 0;
            for (Resource res : syncChunk.getResources()) {
                if (res.getUpdateSequenceNum() > husn) {
                    husn = res.getUpdateSequenceNum();
                }
                ElemInfo resInfo = new ElemInfo();
                resInfo.guid = res.getGuid();
                resInfo.usn = res.getUpdateSequenceNum();
                Callable<Boolean> callable = new RetrieveAndAddResourceTask(resInfo, util);
                final Future<Boolean> task = RP.submit(callable);
                tasks.add(task);
            }
        }
        return tasks;
    }

    private List<Future<Boolean>> createAndSubmitAddNoteTasks(final SyncChunk syncChunk) {
        final List<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();
        Collection<ElemInfo> sc = util.getNotesFrom(syncChunk);
        if (sc.size() > 0) {
            for (ElemInfo note : sc) {
                Callable<Boolean> callable = new RetrieveAndAddNoteTask(note, util);
                final Future<Boolean> task = RP.submit(callable);
                tasks.add(task);
            }
        }
        return tasks;
    }

    public Future<Boolean> downloadNote(String noteguid) {
        ElemInfo note = new ElemInfo();
        note.guid = noteguid;
        note.usn = Integer.MAX_VALUE;
        Callable<Boolean> callable = new RetrieveAndAddNoteTask(note, util);
        final Future<Boolean> task = RP.submit(callable);
        return task;
    }

    private List<Future<Boolean>> createAndSubmitAddExpungedNoteTasks(final SyncChunk syncChunk) {
        final List<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();
        if (syncChunk.isSetExpungedNotes()) {
            for (final String noteguid : syncChunk.getExpungedNotes()) {
                Callable<Boolean> callable = new Callable<Boolean>() {

                    public Boolean call() throws Exception {
                        Lookup.getDefault().lookup(NoteFinder.class).removeByGuid(noteguid);
                        Lookup.getDefault().lookup(NoteRepository.class).deleteNoteByGuid(noteguid);
                        return true;
                    }
                };
                final Future<Boolean> task = RP.submit(callable);
                tasks.add(task);
            }
        }
        return tasks;
    }

    /**
     *
     * @param tasks
     * @param husn
     * @return true if all task were completed sucessfully, false otherwise
     */
    private boolean waitAndUpdateUsn(final List<Future<Boolean>> tasks, int husn) {
        if (!waitForAllTaskToComplete(tasks)) {
            for (Future<Boolean> future : tasks) {
                future.cancel(true);
            }
            setSyncFailed(true);
            return false;
        } else {
            setUSN(husn);
        }
        return true;
    }

    private boolean waitForAllTaskToComplete(final List<Future<Boolean>> tasks) {
        long start = System.currentTimeMillis();
        final int total = tasks.size();
        LOG.info("wait for " + total + " download tasks.");
        try {
            int i = 0;
            for (Future<Boolean> future : tasks) {
                i++;
                LOG.info("get  (" + i + "/" + total + ") from " + future);
                boolean suceeded = future.get(1, TimeUnit.DAYS);
                if (!suceeded) {
                    return false;
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Sync couldn't complete because of:", ex);
            return false;
        }
        long delta = System.currentTimeMillis() - start;
        LOG.info("It took " + delta + " ms");
        return true;
    }

    /**
     * Get the value of PendingRemoteUpdateNotes
     *
     * @return the value of PendingRemoteUpdateNotes
     */
    public int getPendingRemoteUpdateNotes() {
        return PendingRemoteUpdateNotes;
    }

    private void setPendingRemoteUpdateNotes(int PendingRemoteUpdateNotes) {
        int oldValue = this.PendingRemoteUpdateNotes;
        this.PendingRemoteUpdateNotes = PendingRemoteUpdateNotes;
        propertyChangeSupport.firePropertyChange(PROP_PENDINGREMOTEUPDATENOTES, oldValue, this.PendingRemoteUpdateNotes);
    }

    /**
     * Get the value of syncFailed
     *
     * @return the value of syncFailed
     */
    public boolean isSyncFailed() {
        return syncFailed;
    }

    /**
     * Set the value of syncFailed
     *
     * @param syncFailed new value of syncFailed
     */
    private void setSyncFailed(boolean syncFailed) {
        boolean oldSyncFailed = this.syncFailed;
        this.syncFailed = syncFailed;
        propertyChangeSupport.firePropertyChange(PROP_SYNCFAILED, oldSyncFailed, syncFailed);
    }

    /**
     * Add PropertyChangeListener.
     *i
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private boolean isFirstSync() {
        long lastSync = NbPreferences.forModule(SynchronizationServiceImpl.class).getLong(LASTSYNC, 0);
        LOG.info("Last sync was " + new Date(lastSync).toString());
        return lastSync == 0;
    }

    private void setLastSync(long currentTimeMillis) {
        NbPreferences.forModule(SynchronizationServiceImpl.class).putLong(LASTSYNC, currentTimeMillis);
    }

    private int getHighestUSN() {
        int highestUSN = NbPreferences.forModule(SynchronizationServiceImpl.class).getInt(HIGHESTUSN, 0);
        LOG.fine("Highest USN is " + highestUSN);
        return highestUSN;
    }

    private void setUSN(int chunkHighUSN) {
        LOG.fine("setUSN: getHighestUSN=" + getHighestUSN() + " chunkHighUSN=" + chunkHighUSN);
        if (chunkHighUSN > getHighestUSN()) {
            synchronized (this) {
                if (chunkHighUSN > getHighestUSN()) {
                    LOG.info("Updating USN from " + getHighestUSN() + " to " + chunkHighUSN);
                    NbPreferences.forModule(SynchronizationServiceImpl.class).putInt(HIGHESTUSN, chunkHighUSN);
                }
            }
        }
    }

    public void close() {
        RP.shutdownNow();
    }
}

class RetrieveAndAddNoteTask implements Callable<Boolean> {

    private final Logger LOG = Logger.getLogger(RetrieveAndAddNoteTask.class.getName());
    private final String noteGuid;
    private final int usn;
    private final EvernoteProtocolUtil util;

    RetrieveAndAddNoteTask(ElemInfo note, EvernoteProtocolUtil util) {
        this.noteGuid = note.guid;
        this.usn = note.usn;
        this.util = util;
    }

    @Override
    public Boolean call() throws Exception {
        long start = System.currentTimeMillis();
        if (!isUpToDate()) {
            LOG.fine("Start downloading note " + noteGuid);
            //EvernoteProtocolUtil util = EvernoteProtocolUtil.getInstance();
            com.rubenlaguna.en4j.noteinterface.NoteReader note = null;
            try {
                note = util.getNote(noteGuid, true, true, true, true);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Couldn't retrieve note " + noteGuid, ex);
                return null;
            }
            long delta = System.currentTimeMillis() - start;
            final String guid = note.getGuid();
            LOG.info("It took " + delta + " ms" + " to download note " + guid);
            return addToDb(note);
        } else {
            return true;
        }
    }

    private boolean addToDb(com.rubenlaguna.en4j.noteinterface.NoteReader note) {
        final NoteFinder nf = Lookup.getDefault().lookup(NoteFinder.class);
        final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
        boolean suceeded = nr.add(note);
        if (suceeded) {
            final String guid = note.getGuid();
            final com.rubenlaguna.en4j.noteinterface.Note byGuid = nr.getByGuid(guid, false);
            if (null == byGuid) {
                LOG.warning("the note " + guid + " was not added properly to the db");
                return false;
            }
            nf.index(byGuid); //non blocking
            return true;
        } else {
            LOG.log(Level.WARNING, "Fail to add Note \"" + note.getTitle() + "\" to database");
            return false;
        }
    }

    private boolean isUpToDate() {
        final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
        return nr.isNoteUpToDate(noteGuid, usn);
    }
}

class RetrieveAndAddResourceTask implements Callable<Boolean> {

    private final Logger LOG = Logger.getLogger(RetrieveAndAddNoteTask.class.getName());
    private final String resGuid;
    private final int usn;
    private final EvernoteProtocolUtil util;

    RetrieveAndAddResourceTask(ElemInfo res, EvernoteProtocolUtil util) {
        this.resGuid = res.guid;
        this.usn = res.usn;
        this.util = util;
    }

    @Override
    public Boolean call() throws Exception {
        long start = System.currentTimeMillis();
        if (!isUpToDate()) {

            LOG.fine("Start downloading res " + resGuid);
            //EvernoteProtocolUtil util = EvernoteProtocolUtil.getInstance();
            com.rubenlaguna.en4j.noteinterface.Resource res = null;
            try {
                res = util.getResource(resGuid, true, true, true, true);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Couldn't retrieve resource " + resGuid, ex);
                return null;
            }
            final String guid = res.getGuid();
            long delta = System.currentTimeMillis() - start;
            LOG.info("It took " + delta + " ms" + " to download res " + guid);
            final String noteguid = res.getNoteguid();
            final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
            final com.rubenlaguna.en4j.noteinterface.Note byGuid = nr.getByGuid(noteguid, false);
            if (byGuid == null) {
                LOG.info("The parent note is missing from the database download it too");
                ElemInfo ei = new ElemInfo();
                ei.guid = noteguid;
                ei.usn = Integer.MAX_VALUE;
                new RetrieveAndAddNoteTask(ei, util).call();
            }
            return addToDb(res);
        } else {
            return true;
        }
    }

    private boolean addToDb(com.rubenlaguna.en4j.noteinterface.Resource res) {
        final NoteFinder nf = Lookup.getDefault().lookup(NoteFinder.class);
        final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
        boolean suceeded = nr.add(res);
        if (suceeded) {
            final String guid = res.getNoteguid();
            final com.rubenlaguna.en4j.noteinterface.Note byGuid = nr.getByGuid(guid, false);
            if (null == byGuid) {
                LOG.warning("the resource was not added properly to the db we can't find the parent note (" + guid + ")");
                return false;
            }
            nf.index(byGuid); //non blocking
            return true;
        } else {
            LOG.log(Level.WARNING, "Fail to add resoruce \"" + res.getGuid() + "\" to database");
            return false;
        }
    }

    private boolean isUpToDate() {
        final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
        return nr.isResourceUpToDate(resGuid, usn);
    }
}
