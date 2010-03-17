/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ecerulm
 */
public class NoteFinderLuceneImplTest {

    private static NoteFinderLuceneImpl instance;

    public NoteFinderLuceneImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
        Logger logger = Logger.getLogger(NoteFinderLuceneImpl.class.getName());
        logger.setLevel(Level.ALL);

        TestNoteRepository testNoteRepository = new TestNoteRepository();
        testNoteRepository.add(new AbstractNote(1, "nota1", "Sweet Potato Pie"));
        testNoteRepository.add(new AbstractNote(2, "nota2", "Mash four potatoes together"));
        testNoteRepository.add(new AbstractNote(3, "nota3", " internamente thread.isInterrupted()<en-media width=\"125\""
                + "height=\"125\" hash=\"cfc297f6c812543e366da3a070fba4ea\" type=\"image/jpeg\" alt=\"thmb_small_img_6932.jpg\"/>"));
        instance = new NoteFinderLuceneImpl(testNoteRepository);
        instance.setInfoStream(System.out);
        instance.rebuildIndex(null);
        assertEquals(testNoteRepository.getAllNotes().size(), instance.getNumDocs());
        System.out.print("instance.getNumDocs() =" + instance.getNumDocs());
    }

    /**
     * Test of find method, of class NoteFinderLuceneImpl.
     */
    @Test
    public void testFind() {
        System.out.println("find");
        Collection result = instance.find("Swee");
        assertEquals(1, result.size());
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
}
