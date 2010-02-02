/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import java.io.IOException;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ecerulm
 */
public class LuceneTest {

    private RAMDirectory directory;
    private IndexSearcher searcher;

    public LuceneTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        directory = new RAMDirectory();
        final CustomAnalyzer analyzer = new CustomAnalyzer();

        IndexWriter writer = new IndexWriter(directory, analyzer, true, IndexWriter.MaxFieldLength.LIMITED);

        Document doc = new Document();
        final String text = "the Thread.isInterrupted is not supported";
        doc.add(new Field("content", text, Field.Store.NO, Field.Index.ANALYZED));
        AnalyzerUtils.displayTokensWithFullDetails(analyzer, text);
        writer.addDocument(doc);
        writer.close();
        searcher = new IndexSearcher(directory, true);
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void basic() throws IOException {
        Query query = new TermQuery(new Term("content", "thread"));
        TopDocs hits = searcher.search(query, 5);
        assertEquals(1, hits.totalHits);

    }

    @Test
    public void basic2() throws IOException {
        Query query = new TermQuery(new Term("content", "isinterrupted"));
        TopDocs hits = searcher.search(query, 5);
        assertEquals(1, hits.totalHits);

    }

    @Test
    public void withperiod() throws IOException {
        Query query = new TermQuery(new Term("content", "thread.isinterrupted"));
        TopDocs hits = searcher.search(query, 5);
        assertEquals(1, hits.totalHits);

    }
}
