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
    private static NoteFinderLuceneImpl instance;

//    private final TestNoteRepository testNoteRepository = new TestNoteRepository();
    private Lookup lookup;

    public NoteFinderLuceneImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        //System.setProperty("org.openide.util.Lookup", "com.rubenlaguna.en4j.searchlucene.NoteFinderLuceneImplTest");
   
         System.out.println(Lookup.getDefault());
        TestNoteRepository testNoteRepository = Lookup.getDefault().lookup(TestNoteRepository.class);
        testNoteRepository.add(new AbstractNote() {

            public String getContent() {

                return "<?xml version=\"1.0\" encoding=\"UTF - 8\"?>"
                        + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml.dtd\">"
                        + "        <en-note>Parece ser que internamente Searcher.search() y los"
                        + " demas en apache lucene java hacen caso del thread.isInterrupted()"
                        + " asi que si hace un FutureTask.cancel o un RequestProcesor.Task.cancel()"
                        + " deberia pararse la busqueda en el IndexSearcher o el que se este"
                        + " utilizando.  <div><br /></div><div><br /></div><div>el RequestProcessor"
                        + " tiene que haberse creado con el constructor especial para interrupt."
                        + " Si no el .cancel no llama a Thread.interrupt(). <en-media width=\"125\""
                        + "height=\"125\" hash=\"cfc297f6c812543e366da3a070fba4ea\" type=\"image/jpeg\" alt=\"thmb_small_img_6932.jpg\"/>"
                        + "</div></en-note>";
            }

            public Integer getId() {
                return 1;
            }

            public String getTitle() {
                return "lucene honors thread.interrupt";
            }
        });
        testNoteRepository.add(new AbstractNote() {

            public String getContent() {

                return "<?xml version=\"1.0\" encoding=\"UTF - 8\"?>"
                        + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml.dtd\">"
                        + "        <en-note>Sweet Potato Pie</en-note>";
            }

            public Integer getId() {
                return 2;
            }

            public String getTitle() {
                return "example1";
            }
        });
        testNoteRepository.add(new AbstractNote() {

            public String getContent() {

                return "<?xml version=\"1.0\" encoding=\"UTF - 8\"?>"
                        + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml.dtd\">"
                        + "        <en-note>Mash four potatoes together</en-note>";
            }

            public Integer getId() {
                return 3;
            }

            public String getTitle() {
                return "example1";
            }
        });
         instance = new NoteFinderLuceneImpl();
        instance.rebuildIndex(null);
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

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind() {
        System.out.println("find");
        String searchText = "thread*";
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
        System.out.println("find2");
        String searchText = "\"thread.isinterrupted\"";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFindPeriod() {
        System.out.println("testFindPeriod");
        String searchText = "thread.isinterrupted";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }
    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFindPeriod2() {
        System.out.println("testFindPeriod");
        String searchText = "thread. isinterrupted";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }
    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFindPeriodQuotes() {
        System.out.println("testFindPeriod");
        String searchText = "\"thread.isinterrupted\"";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind2Terms() {
        System.out.println("testFindPeriod");
        String searchText = "thread isinterrupted";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind3() {
        System.out.println("find");
        String searchText = "thread";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind4() {
        System.out.println("find");
        String searchText = "threa interna";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind5() {
        System.out.println("find");
        String searchText = "threa interna";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind6() {
        System.out.println("find");
        String searchText = "isinterrupted";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind7() {
        System.out.println("find");
        String searchText = "potato";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(2, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind8() {
        System.out.println("find");
        String searchText = "\"potato\"";
        Collection expResult = null;
        Collection result = instance.find(searchText);
        assertEquals(1, result.size());
        // TODO review the generated test code and remove the default call to fail.
    }
    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFindFilename() {
        System.out.println("findfilename");
        String searchText = "thmb";
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
