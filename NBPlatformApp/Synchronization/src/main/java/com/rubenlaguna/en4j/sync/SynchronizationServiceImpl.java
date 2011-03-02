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
import com.rubenlaguna.en4j.noteinterface.Note;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author ecerulm
 */
@ServiceProvider(service=SynchronizationService.class)
public class SynchronizationServiceImpl implements SynchronizationService {

    public static final String HIGHESTUSN = "highestUSN";
    private static final String LASTSYNC = "lastsync";
    private static final Logger LOG = Logger.getLogger(SynchronizationServiceImpl.class.getName());
    private int PendingRemoteUpdateNotes = 0;
    private static final int MAX_QUEUED_NOTES = 25;
    private final ThreadPoolExecutor tpe = new ThreadPoolExecutor(2, 2, 10, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(MAX_QUEUED_NOTES * 2), new ThreadPoolExecutor.CallerRunsPolicy());
    private final CompletionService<Result> RP = new ExecutorCompletionService<Result>(tpe);
    protected boolean syncFailed = false;
    public static final String PROP_SYNCFAILED = "syncFailed";
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private final EvernoteProtocolUtil util;
    private final static Lock theLock = new ReentrantLock();
//    final Collection<Future<Result>> tasks = new HashSet<Future<Result>>();
//    final Map<Integer> usns = new HashSet<Integer>();
    final Map<Future<Result>, Integer> tasks = new HashMap<Future<Result>, Integer>();
    /**
     * usn pending to be persisted. cannot be written until there is no tasks
     * in tasks with lower USNs
     */
    //private int husn = 0;
    private int lastChunkUsn = 0;
    private SortedSet<Integer> pendingHusn = new TreeSet<Integer>();
    private int fromUSN;

    public SynchronizationServiceImpl() {
        com.rubenlaguna.en4j.sync.Installer.mbean.setThreadPoolExecutor(tpe);
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
        if (theLock.tryLock()) {
            tasks.clear();
            pendingHusn.clear();
            removeTasksFromCompletionService();
            StatusDisplayer.Message currentStatusBarMessage = null;
            try {
                setPendingRemoteUpdateNotes(-1);
                setSyncFailed(false);
                boolean versionOk = util.checkVersion();
                if (!versionOk) {
                    LOG.warning("Incompatible EDAM client protocol version");
                    setSyncFailed(true);
                    return false;
                }

                boolean errorDetected = false;
                boolean moreNotesToDownload = true;
                int initialPendingUpdates = -1;
                do {
                    fromUSN = Math.max(lastChunkUsn, getHighestUSN());

                    LOG.log(Level.INFO, "Highest USN in db {0}, Highest USN last SyncChunk {1} ", new Object[]{getHighestUSN(), lastChunkUsn});
                    LOG.info("retrieving SyncChunk");
                    boolean isFirstSync = isFirstSync();
                    if (isFirstSync) {
                        LOG.info("This is still the first sync.");
                    }
                    final SyncChunk syncChunk = util.getSyncChunk(fromUSN, MAX_QUEUED_NOTES, isFirstSync);
                    int pendingUpdates = util.getUpdateCount() - fromUSN;
                    if (initialPendingUpdates == -1) {
                        initialPendingUpdates = pendingUpdates;
                    }
                    final int chunkHighUSN = syncChunk.getChunkHighUSN();
                    lastChunkUsn = chunkHighUSN;
                    LOG.log(Level.INFO, "highestUSN {0} updateCount {1} SyncChunk.husn {2}", new Object[]{fromUSN, util.getUpdateCount(), chunkHighUSN});
                    final int percentage = (int) ((1.0 - ((float) pendingUpdates / initialPendingUpdates)) * 100);
                    currentStatusBarMessage = StatusDisplayer.getDefault().setStatusText("Downloading notes (" + percentage + " %)", 1);

                    setPendingRemoteUpdateNotes(pendingUpdates);

                    if (chunkHighUSN > fromUSN) {
                        createAndSubmitAddNoteTasks(syncChunk);
                        createAndSubmitAddResourceTasks(syncChunk);
                        createAndSubmitAddExpungedNoteTasks(syncChunk);

                        if (!waitAndUpdateUsn(MAX_QUEUED_NOTES)) {
                            return false;
                        }
                    } else {
                        LOG.info("No notes to download");
                        moreNotesToDownload = false;
                    }
                } while (!errorDetected && moreNotesToDownload);
                waitAndUpdateUsn(0); //wait until ALL taks complete
                setLastSync(System.currentTimeMillis());
                return !errorDetected;
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Sync couldn't complete because of", ex);
                return false;
            } finally {
                if (currentStatusBarMessage != null) {
                    currentStatusBarMessage.clear(500);
                }
                setUSN(0);
                theLock.unlock();
            }
        } else {
            LOG.info("There is another sync running.");
            return false;
        }
    }

    public void createAndSubmitAddResourceTasks(final SyncChunk syncChunk) {
        if (syncChunk.isSetResources()) {
            for (Resource res : syncChunk.getResources()) {
                ElemInfo resInfo = new ElemInfo();
                resInfo.guid = res.getGuid();
                resInfo.usn = res.getUpdateSequenceNum();
                final RetrieveAndAddResourceTask task = new RetrieveAndAddResourceTask(resInfo, util);
                final Future<Result> future = RP.submit(task);
                final int usn = (res.getUpdateSequenceNum() > 0) ? res.getUpdateSequenceNum() : fromUSN - 1;
                tasks.put(future, usn);
            }
        }
    }

    private void createAndSubmitAddNoteTasks(final SyncChunk syncChunk) {
        Collection<ElemInfo> sc = util.getNotesFrom(syncChunk);
        if (sc.size() > 0) {
            for (ElemInfo note : sc) {
                downloadNote(note);

            }
        }
    }

    public Future<Result> downloadNote(String noteguid) {
        ElemInfo note = new ElemInfo();
        note.guid = noteguid;
        note.usn = Integer.MAX_VALUE;
        return downloadNote(note);
    }

    private Future<Result> downloadNote(ElemInfo note) {
        RetrieveAndAddNoteTask task = new RetrieveAndAddNoteTask(note, util);
        final Future<Result> future = RP.submit(task);
        final int usn = (task.getUsn() > 0) ? task.getUsn() : fromUSN - 1;
        tasks.put(future, usn);
        return future;
    }

    private void createAndSubmitAddExpungedNoteTasks(final SyncChunk syncChunk) {
        if (syncChunk.isSetExpungedNotes()) {
            for (final String noteguid : syncChunk.getExpungedNotes()) {
                Callable<Result> callable = new Callable<Result>() {

                    public Result call()
                            throws Exception {
                        Lookup.getDefault().lookup(NoteFinder.class).removeByGuid(noteguid);
                        Lookup.getDefault().lookup(NoteRepository.class).deleteNoteByGuid(noteguid);
                        return new ResultImpl(0, true);
                    }
                };
                final Future<Result> task = RP.submit(callable);
                tasks.put(task, fromUSN - 1);
            }
        }
    }

    private void removeCompletedTasks() {
        Collection<Future<Result>> toRemove = CollectionUtils.select(tasks.keySet(), new Predicate() {

            public boolean evaluate(Object object) {
                //only remove tasks completed successfully
                Future<Result> f = (Future<Result>) object;
                if (!f.isDone()) {
                    return false; //still running task
                }
                if (f.isCancelled()) {
                    return false;
                }
                Result r;
                try {
                    r = f.get();
                } catch (Exception ex) {
                    return false;
                }
                if (r == null) {
                    return false;
                }
                if (!r.isSucceeded()) {
                    return false;
                }
                return true;
            }
        });
        for (Future<Result> f : toRemove) {
            tasks.remove(f);
        }
    }

    private void removeOldPendings() {
        Collection<Integer> expired = CollectionUtils.select(pendingHusn, new Predicate() {

            final int h = getHighestUSN();

            public boolean evaluate(Object object) {
                return ((Integer) object).compareTo(h) <= 0;
            }
        });
        pendingHusn.removeAll(expired);
    }

    private boolean tryPendings() {
        int minUsnInTasks = Integer.MAX_VALUE;
        for (Integer i : tasks.values()) {
            if (i < minUsnInTasks) {
                minUsnInTasks = i;
            }
        }
        LOG.log(Level.INFO, "Min USN in tasks {0}", new Object[]{minUsnInTasks});
        for (int i : pendingHusn) {
            LOG.log(Level.INFO, "Trying pending value {0} minUsnInTasks: {1} getHighestUSN:{2}", new Object[]{i, minUsnInTasks, getHighestUSN()});
            if (i > minUsnInTasks) {
                return true;
            }
            synchronized (this) {
                if (i > getHighestUSN()) {
                    LOG.log(Level.INFO, "setUSN: Updating USN from {0} to {1}", new Object[]{getHighestUSN(), i});
                    NbPreferences.forModule(SynchronizationServiceImpl.class).putInt(HIGHESTUSN, i);
                }
            }
        }
        return false;
    }

    /**
     * Wait until task.size is less that MAX_QUEUED_NOTES
     * @param tasks
     * @param husn
     * @return true if all tasks that completed did so successfully, false otherwise
     */
    private boolean waitAndUpdateUsn(int threshold) {
        if (!waitForSomeTasksToComplete(threshold)) {
            for (Future<Result> future : tasks.keySet()) {
                future.cancel(true);
            }
            setSyncFailed(true);
            return false;
        }
        return true;
    }

    private boolean waitForSomeTasksToComplete(int threshold) {
        long start = System.currentTimeMillis();

        try {
            int i = 0;
            while (tasks.size() > threshold) {
                final int total = tasks.size();
                Future<Result> future = RP.take();
                while (future != null) { //consume all futures finished
                    tasks.remove(future);
                    i++;
                    LOG.log(Level.INFO, "get future ({0}/{1}) ", new Object[]{i, total});
                    boolean suceeded = future.get().isSucceeded();
                    if (!suceeded) {
                        LOG.log(Level.WARNING, "task failed! {0}", future);
                        return false;
                    }
                    setUSN(future.get().getUsn());
                    future = RP.poll();
                }

            }
            LOG.log(Level.INFO, "removed {0} tasks ", new Object[]{i});
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Sync couldn't complete because of:", ex);
            return false;
        }
        long delta = System.currentTimeMillis() - start;
        LOG.log(Level.INFO, "It took {0} ms", delta);
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
        LOG.log(
                Level.INFO, "Last sync was {0}", new Date(lastSync).toString());
        return lastSync == 0;
    }

    private void setLastSync(long currentTimeMillis) {
        NbPreferences.forModule(SynchronizationServiceImpl.class).putLong(LASTSYNC, currentTimeMillis);
    }

    private int getHighestUSN() {
        int highestUSN = NbPreferences.forModule(SynchronizationServiceImpl.class).getInt(HIGHESTUSN, 0);
        LOG.log(
                Level.FINE, "Highest USN is {0}", highestUSN);
        return highestUSN;
    }

    private void setUSN(final int husn) {
        LOG.log(Level.INFO, "setUSN: HighestUSN persisted={0}/ want to update to ={1} / pending {2}", new Object[]{getHighestUSN(), husn, pendingHusn.size()});
        pendingHusn.add(husn);
        removeCompletedTasks();
        removeOldPendings();
        tryPendings();
        removeOldPendings();
        int minInPending = -1;
        int maxInPending = -1;
        if (!pendingHusn.isEmpty()) {
            minInPending = pendingHusn.first();
            maxInPending = pendingHusn.last();
        }
        LOG.log(Level.INFO, "setUSN: pending {0}, min:{1} max:{2} ", new Object[]{pendingHusn.size(), minInPending, maxInPending});
    }

    public void close() {
        tpe.shutdownNow();
    }

    private void removeTasksFromCompletionService() {
        Future<Result> f = RP.poll();
        while (f != null) {
            f = RP.poll(); //non-blocking
        }
    }
}

class RetrieveAndAddNoteTask implements Callable<SynchronizationService.Result> {

    private static final Logger LOG = Logger.getLogger(RetrieveAndAddNoteTask.class.getName());
    private final String noteGuid;
    private int usn;
    private final EvernoteProtocolUtil util;

    RetrieveAndAddNoteTask(ElemInfo note, EvernoteProtocolUtil util) {
        this.noteGuid = note.guid;
        this.usn = note.usn;
        this.util = util;
    }

    @Override
    public SynchronizationService.Result call() throws Exception {
        long start = System.currentTimeMillis();
        if (!isUpToDate()) {
            LOG.log(Level.FINE, "Start downloading note {0}", noteGuid);
            //EvernoteProtocolUtil util = EvernoteProtocolUtil.getInstance();
            com.rubenlaguna.en4j.noteinterface.NoteReader note = null;
            try {
                note = util.getNote(noteGuid, true, true, true, true);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Couldn't retrieve note " + noteGuid, ex);
                return new ResultImpl(0, false);
            }
            long delta = System.currentTimeMillis() - start;
            final String guid = note.getGuid();
            this.usn = note.getUpdateSequenceNumber();
            LOG.log(Level.INFO, "It took {0}" + " ms" + " to download note " + "{1} usn:{2}", new Object[]{delta, guid, usn});
            final boolean succeeded = addToDb(note);
            return new ResultImpl(this.usn, succeeded);
        } else {
            LOG.log(Level.INFO, "Note {0} is up-to-date no need to download it", noteGuid);
            return new ResultImpl(this.usn, true);
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
                LOG.log(Level.WARNING, "the note {0} was not added properly to the db", guid);
                return false;
            }
            nf.index(byGuid); //non blocking
            this.usn = byGuid.getUpdateSequenceNumber();
            return true;
        } else {
            LOG.log(Level.WARNING, "Fail to add Note \"{0}\" to database", note.getTitle());
            return false;
        }
    }

    private boolean isUpToDate() {
        final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
        Note byGuid = nr.getByGuid(noteGuid, false);
        if (byGuid != null) {
            this.usn = byGuid.getId();
        }
        return nr.isNoteUpToDate(noteGuid, usn);
    }

    public int getUsn() {
        return usn;
    }
}

class RetrieveAndAddResourceTask implements Callable<SynchronizationService.Result> {

    private static final Logger LOG = Logger.getLogger(RetrieveAndAddNoteTask.class.getName());
    private final String resGuid;
    private int usn;
    private final EvernoteProtocolUtil util;

    RetrieveAndAddResourceTask(ElemInfo res, EvernoteProtocolUtil util) {
        this.resGuid = res.guid;
        this.usn = res.usn;
        this.util = util;
    }

    @Override
    public SynchronizationService.Result call() throws Exception {
        long start = System.currentTimeMillis();
        if (!isUpToDate()) {

            LOG.log(Level.FINE, "Start downloading res {0}", resGuid);
            //EvernoteProtocolUtil util = EvernoteProtocolUtil.getInstance();
            com.rubenlaguna.en4j.noteinterface.Resource res = null;
            try {
                res = util.getResource(resGuid, true, true, true, true);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Couldn't retrieve resource " + resGuid, ex);
                return null;
            }
            final String guid = res.getGuid();
            this.usn = res.getUpdateSequenceNumber();
            long delta = System.currentTimeMillis() - start;
            LOG.log(Level.INFO, "It took {0} ms" + " to download res " + "{1} usn: {2}", new Object[]{delta, guid, this.usn});
            final String noteguid = res.getNoteguid();
            final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
            final com.rubenlaguna.en4j.noteinterface.Note byGuid = nr.getByGuid(noteguid, false);
            if (byGuid == null) {
                LOG.info("The parent note is missing from the database download it too");
                ElemInfo ei = new ElemInfo();
                ei.guid = noteguid;
                ei.usn = Integer.MAX_VALUE;
                int retries = 0;
                while (!new RetrieveAndAddNoteTask(ei, util).call().isSucceeded() && retries < 3) {
                    retries++;
                    LOG.log(Level.SEVERE, "Problem while download the parent note ({0}). Retrying...", noteguid);
                }
            }
            final boolean succeeded = addToDb(res);
            return new ResultImpl(this.usn, succeeded);
        } else {
            return new ResultImpl(this.usn, true);
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
                LOG.log(Level.WARNING, "the resource was not added properly to the db we can''t find the parent note ({0})", guid);
                return false;
            }
            nf.index(byGuid); //non blocking
            this.usn = byGuid.getUpdateSequenceNumber();
            return true;
        } else {
            LOG.log(Level.WARNING, "Fail to add resoruce \"{0}\" to database", res.getGuid());
            return false;
        }
    }

    private boolean isUpToDate() {
        final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
        final Note byGuid = nr.getByGuid(resGuid, false);
        if (byGuid != null) {
            this.usn = byGuid.getUpdateSequenceNumber();
        }
        return nr.isResourceUpToDate(resGuid, usn);
    }

    public int getUsn() {
        return usn;
    }
}

class ResultImpl implements SynchronizationService.Result {

    private final int usn;
    private final boolean succeeded;

    ResultImpl(final int usn, final boolean succeeded) {
        this.usn = usn;
        this.succeeded = succeeded;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public int getUsn() {
        return usn;
    }
}
