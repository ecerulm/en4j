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

import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteStore;
import com.evernote.edam.notestore.SyncChunk;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.User;
import com.evernote.edam.userstore.AuthenticationResult;
import com.evernote.edam.userstore.UserStore;
import com.rubenlaguna.en4j.interfaces.NoteFinder;
import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.interfaces.SynchronizationService;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

/**
 *
 * @author ecerulm
 */
public class SynchronizationServiceImpl implements SynchronizationService {

    private final Logger LOG = Logger.getLogger(SynchronizationServiceImpl.class.getName());
    private int PendingRemoteUpdateNotes = 0;
    private static final int MAX_QUEUED_NOTES = 25;
    private final ThreadPoolExecutor RP = new ThreadPoolExecutor(4, 10, 10, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(MAX_QUEUED_NOTES * 2));
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public SynchronizationServiceImpl() {
        try {
            com.rubenlaguna.en4j.sync.Installer.mbean.setThreadPoolExecutor(RP);

        } catch (Exception ex) {
            //TODO fix exception handling
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public boolean sync() {
        // Set up the UserStore and check that we can talk to the server
        try {
            Util util = Util.getInstance();
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


                final List<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();
                final Iterator<Note> notesIterator = sc.getNotesIterator();
                if (null != notesIterator) {
                    while (notesIterator.hasNext()) {
                        final Note noteWithoutContents = notesIterator.next();

                        Callable<Boolean> callable = new RetrieveAndAddTask(noteWithoutContents);


                        //TODO: java.util.concurrent.RejectedExecutionException
                        final Future<Boolean> task = RP.submit(callable);
                        tasks.add(task);
                    }
                    if (waitForResults(tasks)) { //this waits for result from each task
                        LOG.warning("Synchronization stopped. One of the notes could not be downloaded or added");
                        errorDetected = true;
                        setSyncFailed(true);
                    }
                } else {
                    LOG.info("No notes to download");
                    moreNotesToDownload = false;
                }
            } while (!errorDetected && moreNotesToDownload);
            return true;
        } catch (EDAMSystemException ex) {
            LOG.log(Level.WARNING, "Sync couldn't complete because of", ex);
            return false;
        } catch (TException ex) {
            LOG.log(Level.WARNING, "Sync couldn't complete because of", ex);
            return false;
        } catch (EDAMUserException ex) {
            LOG.log(Level.WARNING, "Sync couldn't complete because of", ex);
            return false;
        } catch (IllegalStateException ex) {
            LOG.log(Level.WARNING, "Sync couldn't complete because of", ex);
            return false;
        }
    }

    private boolean waitForResults(final List<Future<Boolean>> tasks) {
        long start = System.currentTimeMillis();
        boolean error = false;

        LOG.info("wait for " + tasks.size() + " to complete.");
        for (Future<Boolean> future : tasks) {
            LOG.info("get result of future " + future);
            Boolean futureResult;
            try {
                futureResult = future.get(5, TimeUnit.MINUTES);
            } catch (InterruptedException ex) {
                LOG.log(Level.WARNING, "Sync couldn't complete because of", ex);
                error = true;
                break;
            } catch (ExecutionException ex) {
                LOG.log(Level.WARNING, "Sync couldn't complete because of", ex);
                error = true;
                break;
            } catch (TimeoutException ex) {
                LOG.log(Level.WARNING, "Sync couldn't complete because" + future + " timed out");
                error = true;
                break;
            }

            if (!Boolean.TRUE.equals(futureResult)) {
                LOG.warning("Sync couldn't complete because future task " + future);
                error = true;
                break;
            }
        }
        if (error) {
            for (Future<Boolean> future : tasks) {
                future.cancel(true);
            }
        }
        long delta = System.currentTimeMillis() - start;
        LOG.info("checkResult = " + error + " .It took " + delta + " ms");

        return error;
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
    protected boolean syncFailed = false;
    public static final String PROP_SYNCFAILED = "syncFailed";

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

class RetrieveAndAddTask implements Callable<Boolean> {

    private final Logger LOG = Logger.getLogger(RetrieveAndAddTask.class.getName());
    private Note noteWithoutContents = null;

    RetrieveAndAddTask(Note noteWithoutContents) {
        this.noteWithoutContents = noteWithoutContents;
    }

    public Boolean call() throws Exception {
        long start = System.currentTimeMillis();
        LOG.info("retrieving note " + noteWithoutContents.getGuid());
        Util util = Util.getInstance();
        Note note = null;
        try {
            note = util.getValidNoteStore().getNote(util.getValidAuthToken(), noteWithoutContents.getGuid(), true, true, true, true);
        } catch (TTransportException ex) {
            LOG.log(Level.WARNING, "Couldn't retrieve note " + noteWithoutContents.getGuid(), ex);
            return false;
        }
        long delta = System.currentTimeMillis() - start;
        final String guid = note.getGuid();
        LOG.info("retrieving " + guid + " took " + delta + " ms");
        LOG.info("adding to local database note =" + note.getTitle());
        final NoteAdapter noteAdapter = new NoteAdapter(note);
        final NoteFinder nf = Lookup.getDefault().lookup(NoteFinder.class);
        final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
            
        boolean suceeded = nr.add(noteAdapter);
        if (suceeded) {
            final com.rubenlaguna.en4j.noteinterface.Note byGuid = nr.getByGuid(guid, false);
            if (null == byGuid) {
                LOG.warning("the note " + guid + " was not added properly to the db");
                return false;
            }
            nf.index(byGuid);
        } else {
            LOG.warning("the note " + guid + " was not added properly to the db");
        }
        return suceeded;
    }
}

class Util {

    private final Logger LOG = Logger.getLogger(Util.class.getName());
//    private final String userStoreUrl = "https://sandbox.evernote.com/edam/user";
//    private final String noteStoreUrlBase = "http://sandbox.evernote.com/edam/note/";
    private final String userStoreUrl = "https://www.evernote.com/edam/user";
    private final String noteStoreUrlBase = "http://www.evernote.com/edam/note/";
    // 8-byte Salt
    static final byte[] salt = {
        (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
        (byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03
    };
    // Iteration count
    final int iterationCount = 19;
    private final String consumerKey = "h7ZGlOYrZCo=";
    private final String consumerSecret = "IP/dKyK3226VyqE1ndtj8/JyUwYt1kOq";
    private String a;
    private String b;
    private static final ThreadLocal<NoteStore.Client> currentNoteStore = new ThreadLocal<NoteStore.Client>();
    private static final ThreadLocal<AuthenticationResult> currentAuthResult = new ThreadLocal<AuthenticationResult>();
    private static final ThreadLocal<UserStore.Client> currentUserStore = new ThreadLocal<UserStore.Client>();
    private static final ThreadLocal<String> noteStoreUrl = new ThreadLocal<String>();
    private static final ThreadLocal<Long> expirationTime = new ThreadLocal<Long>() {

        @Override
        protected Long initialValue() {
            return 0L;
        }
    };
    private static final ThreadLocal<String> currentAuthToken = new ThreadLocal<String>() {

        @Override
        protected String initialValue() {
            return "";
        }
    };
    private static Util theInstance;

    private Util() {
        try {
            KeySpec keySpec = new PBEKeySpec("55xdfsfAxkioou546bnTrjk".toCharArray(), salt, iterationCount);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
            Cipher dcipher = Cipher.getInstance(key.getAlgorithm());
            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);
            dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
            byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(consumerKey);
            byte[] utf8 = dcipher.doFinal(dec);
            a = new String(utf8, "UTF8");
            dec = new sun.misc.BASE64Decoder().decodeBuffer(consumerSecret);
            utf8 = dcipher.doFinal(dec);
            b = new String(utf8, "UTF8");
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static synchronized Util getInstance() {
        if (theInstance == null) {
            theInstance = new Util();
        }
        return theInstance;
    }

    private AuthenticationResult getValidAuthenticationResult() throws TTransportException, EDAMUserException, EDAMSystemException, TException {
        if (null == currentAuthResult.get()) {
            LOG.info("First authentication.");
            final String c = a;
            final String d = b;
            final String password = NbPreferences.forModule(SynchronizationServiceImpl.class).get("password", "");
            final String username = NbPreferences.forModule(SynchronizationServiceImpl.class).get("username", "");
            currentAuthResult.set(getUserStore().authenticate(username, password, c, d));

            setExpirationTime(currentAuthResult.get());
        }

        if (isExpired()) {
            LOG.info("The current AuthenticationResult is about to expire. Getting a new one.");
            currentAuthResult.set(getUserStore().refreshAuthentication(currentAuthToken.get()));
            setExpirationTime(
                    currentAuthResult.get());
        }
        currentAuthToken.set(currentAuthResult.get().getAuthenticationToken());
        LOG.info("authToken: " + currentAuthToken);
        return currentAuthResult.get();
    }

    public String getValidAuthToken() throws TTransportException, EDAMUserException, EDAMSystemException, TException {
        //Check is the current authToken is about to expire
        if (isExpired() || "".equals(currentAuthToken.get())) {
            AuthenticationResult authResult = getValidAuthenticationResult();
            User user = authResult.getUser();
            currentAuthToken.set(authResult.getAuthenticationToken());
            LOG.info("new authtoken: \"" + currentAuthToken + "\"");
        } //LOG.info("currentAuthToken: \""+currentAuthToken+"\"");
        return currentAuthToken.get();
    }

    public NoteStore.Client getValidNoteStore() throws TTransportException, EDAMUserException, EDAMSystemException, TException {
        if (isExpired()) {
            // Set up the NoteStore
            AuthenticationResult authResult = getValidAuthenticationResult();
            if (null == noteStoreUrl.get()) {
                //noteStoreUrl must be cached because we don't get a User in
                //from refreshAuthentication
                User user = authResult.getUser();
                noteStoreUrl.set(noteStoreUrlBase + user.getShardId());
            }
            THttpClient noteStoreTrans = new THttpClient(noteStoreUrl.get());
            noteStoreTrans.setConnectTimeout(30000); //30s
            noteStoreTrans.setReadTimeout(30000);//30s
            TBinaryProtocol noteStoreProt = new TBinaryProtocol(noteStoreTrans);
            currentNoteStore.set(new NoteStore.Client(noteStoreProt, noteStoreProt));

        }
        return currentNoteStore.get();
    }

    private boolean isExpired() {
        final long msToExpiration = expirationTime.get() - System.currentTimeMillis();
        LOG.info("auth is valid for " + msToExpiration / 1000.0 + " seconds more (" + (msToExpiration / (1000.0 * 60)) + " minutes)");
        boolean isExpired = msToExpiration < (5 * 60 * 1000L);
        return isExpired;
    }

    public UserStore.Client getUserStore() throws TTransportException {
        if (null == currentUserStore.get()) {
            THttpClient userStoreTrans = new THttpClient(userStoreUrl);
            TBinaryProtocol userStoreProt = new TBinaryProtocol(userStoreTrans);
            currentUserStore.set(new UserStore.Client(userStoreProt, userStoreProt));
        }
        return currentUserStore.get();
    }

    private void setExpirationTime(AuthenticationResult currentAuthResult) {
        long authStartTime = System.currentTimeMillis();
        long authValidityPeriod = currentAuthResult.getExpiration() - currentAuthResult.getCurrentTime();
        LOG.info("New AuthenticationResult valid for " + authValidityPeriod / 1000.0 + " secs.");
        expirationTime.set(authStartTime + authValidityPeriod);
    }
}
