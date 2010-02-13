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

import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteStore;
import com.evernote.edam.notestore.SyncChunk;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.User;
import com.evernote.edam.userstore.AuthenticationResult;
import com.evernote.edam.userstore.UserStore;
import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.interfaces.SynchronizationService;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.Iterator;
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

    private long expirationTime = 0;
    private String currentAuthToken = "";
    private final String consumerKey = "h7ZGlOYrZCo=";
    private final String consumerSecret = "IP/dKyK3226VyqE1ndtj8/JyUwYt1kOq";
    private String a;
    private String b;
    // 8-byte Salt
    final byte[] salt = {
        (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
        (byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03
    };
    // Iteration count
    final int iterationCount = 19;
//    private final String userStoreUrl = "https://sandbox.evernote.com/edam/user";
//    private final String noteStoreUrlBase = "http://sandbox.evernote.com/edam/note/";
    private final String userStoreUrl = "https://www.evernote.com/edam/user";
    private final String noteStoreUrlBase = "http://www.evernote.com/edam/note/";
    private final RequestProcessor RP = new RequestProcessor("Sync thread", 1, true);
    private final Logger LOG = Logger.getLogger(SynchronizationServiceImpl.class.getName());
    private NoteStore.Client currentNoteStore = null;
    private AuthenticationResult currentAuthResult = null;
    private UserStore.Client currentUserStore;

    public SynchronizationServiceImpl() {
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
            //TODO fix exception handling
            Exceptions.printStackTrace(ex);
        }


    }

    @Override
    public boolean sync() {
        // Set up the UserStore and check that we can talk to the server
        try {

            UserStore.Client userStore = getUserStore();
            boolean versionOk = userStore.checkVersion("Evernote's EDAMDemo (Java)", com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR, com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
            if (!versionOk) {
                LOG.warning("Incompatible EDAM client protocol version");
                return false;
            }

            NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
            boolean finished = false;
            do {
                int highestUSN = nr.getHighestUSN();
                LOG.info("highest updateSequenceNumber in the database = " + highestUSN);
                //boolean fullSync = (highestUSN == 0);
                LOG.info("retrieving SyncChunk");
                SyncChunk sc = getValidNoteStore().getSyncChunk(getValidAuthToken(), highestUSN, 25, true);
                LOG.info("SyncChunk retrieved");
                final Iterator<Note> notesIterator = sc.getNotesIterator();
                if (null != notesIterator) {
                    while (notesIterator.hasNext()) {
                        Note noteWithoutContents = notesIterator.next();
                        long start = System.currentTimeMillis();
                        LOG.info("retrieving note " + noteWithoutContents.getGuid());
                        Note note = getValidNoteStore().getNote(getValidAuthToken(), noteWithoutContents.getGuid(), true, true, false, false);
                        long delta = System.currentTimeMillis() - start;
                        LOG.info("retrieving " + note.getGuid() + " took " + delta + " ms");
                        LOG.info("adding to local database note =" + note.getTitle());
                        nr.add(new NoteAdapter(note));

                    }
                } else {
                    LOG.info("No notes to download");
                    finished = true;
                }
            } while (!finished);

            return true;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Sync couldn't complete because of", ex);
            return false;
        }
    }

    private AuthenticationResult getValidAuthenticationResult() throws TTransportException, EDAMUserException, EDAMSystemException, TException {
        if (isExpired()) {
            LOG.info("The current AuthenticationResult is about to expire. Getting a new one.");
            final String c = a;
            final String d = b;
            final String password = NbPreferences.forModule(SynchronizationServiceImpl.class).get("password", "");
            final String username = NbPreferences.forModule(SynchronizationServiceImpl.class).get("username", "");
            long authStartTime = System.currentTimeMillis();
            currentAuthResult = getUserStore().authenticate(username, password, c, d);
            long authValidityPeriod = currentAuthResult.getExpiration() - currentAuthResult.getCurrentTime();
            LOG.info("New AuthenticationResult valid for " + authValidityPeriod / 1000.0 + " secs.");
            LOG.info("authToken: " + currentAuthResult.getAuthenticationToken());
            expirationTime = authStartTime + authValidityPeriod;
        }
        return currentAuthResult;
    }

    private String getValidAuthToken() throws TTransportException, EDAMUserException, EDAMSystemException, TException {
        //Check is the current authToken is about to expire
        if (isExpired() || "".equals(currentAuthToken)) {
            AuthenticationResult authResult = getValidAuthenticationResult();
            User user = authResult.getUser();
            currentAuthToken = authResult.getAuthenticationToken();
            LOG.info("new authtoken: \"" + currentAuthToken + "\"");
        }
        //LOG.info("currentAuthToken: \""+currentAuthToken+"\"");
        return currentAuthToken;
    }

    private NoteStore.Client getValidNoteStore() throws TTransportException, EDAMUserException, EDAMSystemException, TException {
        if (isExpired()) {
            // Set up the NoteStore
            AuthenticationResult authResult = getValidAuthenticationResult();
            User user = authResult.getUser();
            String noteStoreUrl = noteStoreUrlBase + user.getShardId();
            THttpClient noteStoreTrans = new THttpClient(noteStoreUrl);
            TBinaryProtocol noteStoreProt = new TBinaryProtocol(noteStoreTrans);
            currentNoteStore = new NoteStore.Client(noteStoreProt, noteStoreProt);
        }
        return currentNoteStore;
    }

    private boolean isExpired() {
        boolean isExpired = (expirationTime - System.currentTimeMillis()) < 10000;
        return isExpired;
    }

    private UserStore.Client getUserStore() throws TTransportException {
        if (null == currentUserStore) {
            THttpClient userStoreTrans = new THttpClient(userStoreUrl);
            TBinaryProtocol userStoreProt = new TBinaryProtocol(userStoreTrans);
            currentUserStore = new UserStore.Client(userStoreProt, userStoreProt);
        }
        return currentUserStore;
    }
}

