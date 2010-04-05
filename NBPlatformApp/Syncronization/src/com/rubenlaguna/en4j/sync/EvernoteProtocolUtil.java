/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.openide.util.NbPreferences;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class EvernoteProtocolUtil implements EDAMIf {

    private final Logger LOG = Logger.getLogger(EvernoteProtocolUtil.class.getName());
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
    private static final ThreadLocal<Integer> updateCount = new ThreadLocal<Integer>();
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
    private static EvernoteProtocolUtil theInstance;

    private EvernoteProtocolUtil() {
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

    public static synchronized EvernoteProtocolUtil getInstance() {
        if (theInstance == null) {
            theInstance = new EvernoteProtocolUtil();
        }
        return theInstance;
    }

    private AuthenticationResult getValidAuthenticationResult() throws TTransportException, EDAMUserException, EDAMSystemException, TException {
        if (null == currentAuthResult.get() || isExpired()) {
            LOG.info("First authentication or auth token is expired and cannot be refreshed.");
            final String c = a;
            final String d = b;
            final String password = NbPreferences.forModule(SynchronizationServiceImpl.class).get("password", "");
            final String username = NbPreferences.forModule(SynchronizationServiceImpl.class).get("username", "");
            currentAuthResult.set(getUserStore().authenticate(username, password, c, d));
            setExpirationTime(currentAuthResult.get());
            Installer.mbean.incrementReauthCounter();
        }

        if (isAboutToExpire()) {
            //refresh the auth token
            LOG.info("The current AuthenticationResult is about to expire. Getting a new one.");
            currentAuthResult.set(getUserStore().refreshAuthentication(currentAuthToken.get()));
            setExpirationTime(currentAuthResult.get());
            Installer.mbean.incrementReauthCounter();
        }
        currentAuthToken.set(currentAuthResult.get().getAuthenticationToken());
        LOG.fine("authToken: " + currentAuthToken);
        return currentAuthResult.get();
    }

    private String getValidAuthToken() throws TTransportException, EDAMUserException, EDAMSystemException, TException {
        //Check is the current authToken is about to expire
        if (isAboutToExpire() || "".equals(currentAuthToken.get())) {
            AuthenticationResult authResult = getValidAuthenticationResult();
//            User user = authResult.getUser();
            currentAuthToken.set(authResult.getAuthenticationToken());
            LOG.fine("new authtoken: \"" + currentAuthToken + "\"");
        }
        return currentAuthToken.get();
    }

    private NoteStore.Client getValidNoteStore() throws TTransportException, EDAMUserException, EDAMSystemException, TException {
        if (isAboutToExpire()) {
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
            noteStoreTrans.setReadTimeout(180 * 1000);//180s
            TBinaryProtocol noteStoreProt = new TBinaryProtocol(noteStoreTrans);
            currentNoteStore.set(new NoteStore.Client(noteStoreProt, noteStoreProt));

        }
        return currentNoteStore.get();
    }

    private boolean isAboutToExpire() {
        final long msToExpiration = expirationTime.get() - System.currentTimeMillis();
        LOG.fine("auth is valid for " + msToExpiration / 1000.0 + " seconds more (" + (msToExpiration / (1000.0 * 60)) + " minutes)");
        boolean isExpired = msToExpiration < (5 * 60 * 1000L);
        return isExpired;
    }

    private boolean isExpired() {
        final long msToExpiration = expirationTime.get() - System.currentTimeMillis();
        LOG.fine("auth is valid for " + msToExpiration / 1000.0 + " seconds more (" + (msToExpiration / (1000.0 * 60)) + " minutes)");
        boolean isExpired = msToExpiration < (5000L); //less than 5s
        return isExpired;
    }

    private UserStore.Client getUserStore() throws TTransportException {
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

    @Override
    public boolean checkVersion() {
        try {
            return getUserStore().checkVersion("en4j (evernote for java)", com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR, com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "exception caught:", ex);
        }
        return false;
    }

    @Override
    public Collection<NoteInfo> getSyncChunk(int highestUSN, int numnotes, boolean isFirstSync) {
        try {
            SyncChunk sc = getValidNoteStore().getSyncChunk(getValidAuthToken(), highestUSN, numnotes, isFirstSync);
            updateCount.set(sc.getUpdateCount());
            Collection<NoteInfo> toReturn = new ArrayList<NoteInfo>();
            for (Note note : sc.getNotes()) {
                NoteInfo ni = new NoteInfo();
                ni.guid = note.getGuid();
                ni.usn = note.getUpdateSequenceNum();
                toReturn.add(ni);
            }
            return toReturn;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "exception caught:", ex);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public int getUpdateCount() {
        return updateCount.get();
    }

    @Override
    public com.rubenlaguna.en4j.noteinterface.NoteReader getNote(String noteGuid, boolean b, boolean b0, boolean b1, boolean b2) throws Exception {
        Note n = getValidNoteStore().getNote(getValidAuthToken(), noteGuid, true, true, true, true);
        NoteAdapter na = new NoteAdapter(n);
        return na;
    }
}

