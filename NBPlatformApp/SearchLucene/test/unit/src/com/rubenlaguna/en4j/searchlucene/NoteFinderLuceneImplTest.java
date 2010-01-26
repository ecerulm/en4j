/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.util.Collection;
import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import static org.junit.Assert.*;

/**
 *
 * @author ecerulm
 */
public class NoteFinderLuceneImplTest implements Lookup.Provider {

//    private final TestNoteRepository testNoteRepository = new TestNoteRepository();
    private Lookup lookup;

    public NoteFinderLuceneImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        //System.setProperty("org.openide.util.Lookup", "com.rubenlaguna.en4j.searchlucene.NoteFinderLuceneImplTest");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        System.out.println(Lookup.getDefault());
        TestNoteRepository testNoteRepository = Lookup.getDefault().lookup(TestNoteRepository.class);
        testNoteRepository.add(new Note() {

            public String getContent() {

                return "<?xml version=\"1.0\" encoding=\"UTF - 8\"?>"
                        + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml.dtd\">"
                        + "        <en-note>Parece ser que internamente Searcher.search() y los demas en apache lucene java hacen caso del thread.isInterrupted() asi que si hace un FutureTask.cancel o un RequestProcesor.Task.cancel() deberia pararse la busqueda en el IndexSearcher o el que se este utilizando.  <div><br /></div><div><br /></div><div>el RequestProcessor tiene que haberse creado con el constructor especial para interrupt. Si no el .cancel no llama a Thread.interrupt(). </div></en-note>";
            }

            public void setContent(String content) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public Date getCreated() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void setCreated(Date created) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public Integer getId() {
                return 1;
            }

            public void setId(Integer id) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public String getSourceurl() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void setSourceurl(String sourceurl) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public String getTitle() {
                return "lucene honors thread.interrupt";
            }

            public void setTitle(String title) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public Date getUpdated() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void setUpdated(Date updated) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public Resource getResource(String hash) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind() {
        System.out.println("find");
        String searchText = "thread*";
        NoteFinderLuceneImpl instance = new NoteFinderLuceneImpl();
        instance.rebuildIndex();
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind2() {
        System.out.println("find");
        String searchText = "thread.isinterrupted";
        NoteFinderLuceneImpl instance = new NoteFinderLuceneImpl();
        instance.rebuildIndex();
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }

    public Lookup getLookup() {
        System.out.println("getLokup");
        if (lookup == null) {
            lookup = Lookups.singleton(new TestNoteRepository());
        }
        return lookup;
    }
}
