/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.sync;

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
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.THttpClient;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

/**
 *
 * @author ecerulm
 */
public class SynchronizationServiceImpl implements SynchronizationService {

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

    public void sync() {
        final String c = a;
        final String d = b;
        // Set up the UserStore and check that we can talk to the server
        try {
            THttpClient userStoreTrans = new THttpClient(userStoreUrl);
            TBinaryProtocol userStoreProt = new TBinaryProtocol(userStoreTrans);
            UserStore.Client userStore = new UserStore.Client(userStoreProt, userStoreProt);
            boolean versionOk = userStore.checkVersion("Evernote's EDAMDemo (Java)", com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR, com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
            if (!versionOk) {
                System.err.println("Incompatible EDAM client protocol version");
                return;
            }
            final String username = NbPreferences.forModule(SynchronizationServiceImpl.class).get("username", "");
            final String password = NbPreferences.forModule(SynchronizationServiceImpl.class).get("password", "");
            AuthenticationResult authResult = userStore.authenticate(username, password, c, d);
            User user = authResult.getUser();
            String authToken = authResult.getAuthenticationToken();
            // Set up the NoteStore
            String noteStoreUrl = noteStoreUrlBase + user.getShardId();
            THttpClient noteStoreTrans = new THttpClient(noteStoreUrl);
            TBinaryProtocol noteStoreProt = new TBinaryProtocol(noteStoreTrans);
            NoteStore.Client noteStore = new NoteStore.Client(noteStoreProt, noteStoreProt);

            NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
            boolean finished = false;
            do {
                int highestUSN = nr.getHighestUSN();
                LOG.info("highest updateSequenceNumber in the database = " + highestUSN);
                boolean fullSync = (highestUSN == 0);
                LOG.info("retrieving SyncChunk");
                SyncChunk sc = noteStore.getSyncChunk(authToken, highestUSN, 25, true);
                LOG.info("SyncChunk retrieved");
                final Iterator<Note> notesIterator = sc.getNotesIterator();
                if (null != notesIterator) {
                    while (notesIterator.hasNext()) {
                        Note noteWithoutContents = notesIterator.next();
                        long start = System.currentTimeMillis();
                        LOG.info("retrieving note " + noteWithoutContents.getGuid());
                        Note note = noteStore.getNote(authToken, noteWithoutContents.getGuid(), true, true, false, false);
                        long delta = System.currentTimeMillis() - start;
                        LOG.info("retrieving "+note.getGuid()+" took "+delta+" ms");
                        LOG.info("adding to local database note =" + note.getTitle());
                        nr.add(new NoteAdapter(note));

                    }
                } else {
                    LOG.info("No notes to download");
                    finished = true;
                }
            } while (!finished);

        } catch (Exception ex) {
            //TODO handle exceptions
            Exceptions.printStackTrace(ex);
        }

    }
}
