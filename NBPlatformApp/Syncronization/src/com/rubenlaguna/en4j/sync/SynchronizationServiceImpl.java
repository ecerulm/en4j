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
import com.evernote.edam.type.Note;
import com.evernote.edam.userstore.UserStore;
import com.rubenlaguna.en4j.interfaces.NoteFinder;
import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.interfaces.SynchronizationService;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.thrift.transport.TTransportException;
import org.openide.util.Lookup;

/**
 *
 * @author ecerulm
 */
public class SynchronizationServiceImpl implements SynchronizationService {

    private final Logger LOG = Logger.getLogger(SynchronizationServiceImpl.class.getName());
    private int PendingRemoteUpdateNotes = 0;
    private static final int MAX_QUEUED_NOTES = 25;
    private final ThreadPoolExecutor RP = new ThreadPoolExecutor(4, 10, 10, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(MAX_QUEUED_NOTES * 2));
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
            UserStore.Client userStore = util.getUserStore();
            boolean versionOk = userStore.checkVersion("Evernote's EDAMDemo (Java)", com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR, com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
            if (!versionOk) {
                LOG.warning("Incompatible EDAM client protocol version");
                return false;
            }

            final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
            boolean errorDetected = false;
            boolean moreNotesToDownload = true;
            setSyncFailed(false);
            do {
                int highestUSN = nr.getHighestUSN();
                LOG.info("highest updateSequenceNumber in the database = " + highestUSN);
                //boolean fullSync = (highestUSN == 0);
                LOG.info("retrieving SyncChunk");
                SyncChunk sc = util.getValidNoteStore().getSyncChunk(util.getValidAuthToken(), highestUSN, MAX_QUEUED_NOTES, true);
                LOG.info("SyncChunk retrieved");
                int pendingUpdates = sc.getUpdateCount() - highestUSN;
                setPendingRemoteUpdateNotes(pendingUpdates);

                final List<Future<com.rubenlaguna.en4j.noteinterface.Note>> tasks = new ArrayList<Future<com.rubenlaguna.en4j.noteinterface.Note>>();
                final Iterator<Note> notesIterator = sc.getNotesIterator();
                if (null != notesIterator) {
                    retrieveNotesAsync(notesIterator, tasks);
                    if (!addNotesToDb(tasks)) {
                        for (Future<com.rubenlaguna.en4j.noteinterface.Note> future : tasks) {
                            future.cancel(true);
                        }
                        errorDetected = true;
                        setSyncFailed(true);
                    }
                } else {
                    LOG.info("No notes to download");
                    moreNotesToDownload = false;
                }
            } while (!errorDetected && moreNotesToDownload);
            return !errorDetected;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Sync couldn't complete because of", ex);
            return false;
        }
    }

    private void retrieveNotesAsync(final Iterator<Note> notesIterator, final List<Future<com.rubenlaguna.en4j.noteinterface.Note>> tasks) {
        while (notesIterator.hasNext()) {
            final Note noteWithoutContents = notesIterator.next();
            Callable<com.rubenlaguna.en4j.noteinterface.Note> callable = new RetrieveNoteTask(noteWithoutContents);
            //TODO: java.util.concurrent.RejectedExecutionException
            final Future<com.rubenlaguna.en4j.noteinterface.Note> task = RP.submit(callable);
            tasks.add(task);
        }
    }

    private boolean addNotesToDb(final List<Future<com.rubenlaguna.en4j.noteinterface.Note>> tasks) {
        long start = System.currentTimeMillis();
        final int total = tasks.size();
        LOG.info("wait for " + total + " notes to download.");
        try {
            int i = 0;
            for (Future<com.rubenlaguna.en4j.noteinterface.Note> future : tasks) {
                i++;
                LOG.info("get note (" + i + "/" + total + ") of future " + future);
                com.rubenlaguna.en4j.noteinterface.Note note = future.get(1, TimeUnit.DAYS);
                boolean suceeded = addToDb(note);
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
}

class RetrieveNoteTask implements Callable<com.rubenlaguna.en4j.noteinterface.Note> {

    private final Logger LOG = Logger.getLogger(RetrieveNoteTask.class.getName());
    private Note noteWithoutContents = null;

    RetrieveNoteTask(Note noteWithoutContents) {
        this.noteWithoutContents = noteWithoutContents;
    }

    public com.rubenlaguna.en4j.noteinterface.Note call() throws Exception {
        long start = System.currentTimeMillis();
        LOG.fine("Start downloading note " + noteWithoutContents.getGuid());
        EvernoteProtocolUtil util = EvernoteProtocolUtil.getInstance();
        Note note = null;
        try {
            note = util.getValidNoteStore().getNote(util.getValidAuthToken(), noteWithoutContents.getGuid(), true, true, true, true);
        } catch (TTransportException ex) {
            LOG.log(Level.WARNING, "Couldn't retrieve note " + noteWithoutContents.getGuid(), ex);
            return null;
        }
        long delta = System.currentTimeMillis() - start;
        final String guid = note.getGuid();
        LOG.info("It took " + delta + " ms" + " to download note " + guid);
        final NoteAdapter noteAdapter = new NoteAdapter(note);
        return noteAdapter;
    }
}
