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

import com.evernote.edam.type.Note;
import com.rubenlaguna.en4j.interfaces.NoteFinder;
import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.interfaces.SynchronizationService;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private final ThreadPoolExecutor RP = new ThreadPoolExecutor(2, 2, 10, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(MAX_QUEUED_NOTES * 2));
    //private final ExecutorService RPDB = Executors.newSingleThreadExecutor();
    protected boolean syncFailed = false;
    public static final String PROP_SYNCFAILED = "syncFailed";
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public SynchronizationServiceImpl() {
        com.rubenlaguna.en4j.sync.Installer.mbean.setThreadPoolExecutor(RP);
    }

    @Override
    public boolean sync() {
        // Set up the UserStore and check that we can talk to the server
        try {
            EvernoteProtocolUtil util = EvernoteProtocolUtil.getInstance();
//            UserStore.Client userStore = util.getUserStore();
            boolean versionOk = util.checkVersion();
            if (!versionOk) {
                LOG.warning("Incompatible EDAM client protocol version");
                return false;
            }

//            final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
            boolean errorDetected = false;
            boolean moreNotesToDownload = true;
            setSyncFailed(false);
            do {
                int highestUSN = getHighestUSN();
                LOG.info("highest updateSequenceNumber in the database = " + highestUSN);
                //boolean fullSync = (highestUSN == 0);
                LOG.info("retrieving SyncChunk");
                boolean isFirstSync = isFirstSync();
                if (isFirstSync) {
                    LOG.info("This is still the first sync.");
                }
//                SyncChunk sc = util.getValidNoteStore().getSyncChunk(util.getValidAuthToken(), highestUSN, MAX_QUEUED_NOTES, isFirstSync);
//                LOG.info("SyncChunk retrieved");

//                int pendingUpdates = sc.getUpdateCount() - highestUSN;
                Collection<NoteInfo> sc = util.getSyncChunk(highestUSN, MAX_QUEUED_NOTES, isFirstSync);
                int pendingUpdates = util.getUpdateCount() - highestUSN;
                setPendingRemoteUpdateNotes(pendingUpdates);

                final List<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();
                final Iterator<NoteInfo> notesIterator = sc.iterator();
                if (null != notesIterator) {
                    retrieveNotesAsync(notesIterator, tasks);
                    if (!waitForAllTaskToComplete(tasks)) {
                        for (Future<Boolean> future : tasks) {
                            future.cancel(true);
                        }
                        errorDetected = true;
                        setSyncFailed(true);
                    } else {
                        int husn = getHighestUsnInCollection(sc);
                        setUSN(husn);
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
        }
    }

    private int getHighestUsnInCollection(Collection<NoteInfo> sc) {
        int husn = 0;
        for (NoteInfo noteInfo : sc) {
            if (noteInfo.usn > husn) {
                husn = noteInfo.usn;
            }
        }
        return husn;
    }

    private void retrieveNotesAsync(final Iterator<NoteInfo> notesIterator, final List<Future<Boolean>> tasks) {
        while (notesIterator.hasNext()) {
            final NoteInfo note = notesIterator.next();
            Callable<Boolean> callable = new RetrieveAndAddNoteTask(note);
            //TODO: java.util.concurrent.RejectedExecutionException
            final Future<Boolean> task = RP.submit(callable);
            tasks.add(task);
        }
    }

    private boolean waitForAllTaskToComplete(final List<Future<Boolean>> tasks) {
        long start = System.currentTimeMillis();
        final int total = tasks.size();
        LOG.info("wait for " + total + " notes to download.");
        try {
            int i = 0;
            for (Future<Boolean> future : tasks) {
                i++;
                LOG.info("get note (" + i + "/" + total + ") of future " + future);
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
        LOG.info("Higest USN is " + highestUSN);
        return highestUSN;
    }

    private void setUSN(int chunkHighUSN) {
        LOG.info("setUSN: getHighestUSN=" + getHighestUSN() + " chunkHighUSN=" + chunkHighUSN);
        if (chunkHighUSN > getHighestUSN()) {
            synchronized (this) {
                if (chunkHighUSN > getHighestUSN()) {
                    LOG.info("Updating USN from " + getHighestUSN() + " to " + chunkHighUSN);
                    NbPreferences.forModule(SynchronizationServiceImpl.class).putInt(HIGHESTUSN, chunkHighUSN);
                }
            }
        }
    }
}



class RetrieveAndAddNoteTask implements Callable<Boolean> {

    private final Logger LOG = Logger.getLogger(RetrieveAndAddNoteTask.class.getName());
    private final String noteGuid ;
    private final int usn;

    RetrieveAndAddNoteTask(NoteInfo note) {
        this.noteGuid = note.guid;
        this.usn = note.usn;
    }

    @Override
    public Boolean call() throws Exception {
        long start = System.currentTimeMillis();
        if (!isUpToDate()) {
            LOG.fine("Start downloading note " + noteGuid);
            EvernoteProtocolUtil util = EvernoteProtocolUtil.getInstance();
            com.rubenlaguna.en4j.noteinterface.Note note = null;
            try {
                note = util.getNote(noteGuid, true, true, true, true);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Couldn't retrieve note " + noteGuid, ex);
                return null;
            }
            long delta = System.currentTimeMillis() - start;
            final String guid = note.getGuid();
            LOG.info("It took " + delta + " ms" + " to download note " + guid);
//            final NoteAdapter noteAdapter = new NoteAdapter(note);
            return addToDb(note);
        } else {
            return true;
        }
    }

    private boolean addToDb(com.rubenlaguna.en4j.noteinterface.Note note) {
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
        return nr.isUpToDate(noteGuid, usn);
    }
}
