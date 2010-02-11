/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.evernoteapi;

import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.notestore.NoteStore;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.User;
import com.evernote.edam.userstore.AuthenticationResult;
import com.evernote.edam.userstore.UserStore;
import java.util.List;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.THttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ecerulm
 */
public class BasicEvernoteApiTest {

    public BasicEvernoteApiTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void hello() throws Exception {
        String consumerKey = "h7ZGlOYrZCo=";
        String consumerSecret = "IP/dKyK3226VyqE1ndtj8/JyUwYt1kOq";


        String userStoreUrl = "https://sandbox.evernote.com/edam/user";
        String noteStoreUrlBase = "http://sandbox.evernote.com/edam/note/";

//        if (args.length < 2) {
//            System.err.println("Arguments:  <username> <password>");
//            return;
//        }
        String username = "ecerulmtest";
        String password = "ecerulmtest";

        // Set up the UserStore and check that we can talk to the server
        THttpClient userStoreTrans = new THttpClient(userStoreUrl);
        TBinaryProtocol userStoreProt = new TBinaryProtocol(userStoreTrans);
        UserStore.Client userStore = new UserStore.Client(userStoreProt,
                userStoreProt);
        boolean versionOk = userStore.checkVersion("Evernote's EDAMDemo (Java)",
                com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR,
                com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
        if (!versionOk) {
            System.err.println("Incomatible EDAM client protocol version");
            return;
        }

        // Authenticate as a user & password
        AuthenticationResult authResult = userStore.authenticate(username,
                password, consumerKey, consumerSecret);
        User user = authResult.getUser();
        String authToken = authResult.getAuthenticationToken();

        // Set up the NoteStore
        System.out.println("Notes for " + user.getUsername() + ":");
        String noteStoreUrl = noteStoreUrlBase + user.getShardId();
        THttpClient noteStoreTrans = new THttpClient(noteStoreUrl);
        TBinaryProtocol noteStoreProt = new TBinaryProtocol(noteStoreTrans);
        NoteStore.Client noteStore = new NoteStore.Client(noteStoreProt,
                noteStoreProt);

        // List all of the notes in the account
        List<Notebook> notebooks = (List<Notebook>) noteStore.listNotebooks(authToken);
        Notebook defaultNotebook = notebooks.get(0);
        for (Notebook notebook : notebooks) {
            System.out.println("Notebook: " + notebook.getName());
            NoteFilter filter = new NoteFilter();
            filter.setOrder(NoteSortOrder.CREATED.getValue());
            filter.setAscending(true);
            filter.setNotebookGuid(notebook.getGuid());
            NoteList noteList = noteStore.findNotes(authToken, filter, 0, 100);
            List<Note> notes = (List<Note>) noteList.getNotes();
            for (Note note : notes) {
                System.out.println(" * " + note.getTitle());
            }
            if (notebook.isDefaultNotebook()) {
                defaultNotebook = notebook;
            }
        }

//        // Create a note containing a little text, plus the "enlogo.png" image
//        Resource resource = new Resource();
//        resource.setData(readFileAsData("enlogo.png"));
//        resource.setMime("image/png");
//        Note note = new Note();
//        note.setTitle("Test note from EDAMDemo");
//        note.setCreated(System.currentTimeMillis());
//        note.setUpdated(System.currentTimeMillis());
//        note.setActive(true);
//        note.setNotebookGuid(defaultNotebook.getGuid());
//        note.addToResources(resource);
//        String hashHex = bytesToHex(resource.getData().getBodyHash());
//        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
//                + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml.dtd\">"
//                + "<en-note>Here's the Evernote logo:<br/>"
//                + "<en-media type=\"image/png\" hash=\"" + hashHex + "\"/>"
//                + "</en-note>";
//        note.setContent(content);
//        Note createdNote = noteStore.createNote(authToken, note);
//        System.out.println("Note created. GUID: " + createdNote.getGuid());

    }
}
