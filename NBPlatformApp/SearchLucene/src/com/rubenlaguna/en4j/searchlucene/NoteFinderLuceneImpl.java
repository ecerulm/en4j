/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.interfaces.NoteFinder;
import com.rubenlaguna.en4j.interfaces.NoteRepository;

import com.rubenlaguna.en4j.noteinterface.Note;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author ecerulm
 */
public class NoteFinderLuceneImpl implements NoteFinder {

    private DOMFragmentParser domParser = new DOMFragmentParser();
    private static Logger LOG = Logger.getLogger(NoteFinderLuceneImpl.class.getName());

    public Collection<Note> find(String searchText) {
        Collection<Note> toReturn = new ArrayList<Note>();

        try {
            File file = new File(System.getProperty("netbeans.user") + "en4j/luceneindex");
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
            IndexReader reader = IndexReader.open(FSDirectory.open(file), true);

            IndexSearcher searcher = new IndexSearcher(reader);

            QueryParser parser = new QueryParser("title", analyzer);
            Query query = parser.parse(searchText);
            //search the query

            TopDocs topdocs = searcher.search(query, null, 5);
            for (ScoreDoc sd : topdocs.scoreDocs) {
                int scoreId = sd.doc;
                Document document = searcher.doc(scoreId);
                int docId = Integer.parseInt(document.getField("id").stringValue());

                NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
                toReturn.add(rep.get(docId));
            }


        } catch (ParseException ex) {
            Exceptions.printStackTrace(ex);
        } catch (CorruptIndexException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return toReturn;
    }

    public synchronized void  rebuildIndex() {
        IndexWriter writer = null;
        LOG.info("about to start indexing");
        try {
            // TODO implement action body
            File file = new File(System.getProperty("netbeans.user") + "en4j/luceneindex");
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
            writer = new IndexWriter(FSDirectory.open(file),
                    analyzer, true,
                    IndexWriter.MaxFieldLength.LIMITED);
            writer.setUseCompoundFile(true);
            writer.deleteAll();
            writer.commit();
            Collection<Note> notes = getAllNotes();

            LOG.info("number of notes "+notes.size());
            for (Note note : notes) {
                if(Thread.interrupted()) {
                    //if the task has been cancelled we skip the rest of the
                    //notes but we still do the writer.commit()
                    writer.close();
                    return;
                }
                LOG.info("indexing note "+note);
                //Lucene document http://www.darksleep.com/lucene/
                Document document = new Document();

                Field idField = new Field("id",
                        note.getId().toString(),
                        Field.Store.YES,
                        Field.Index.NOT_ANALYZED);
                document.add(idField);

                Field titleField = new Field("title",
                        note.getTitle(),
                        Field.Store.YES,
                        Field.Index.ANALYZED);
                document.add(titleField);

                //according to Lucene in Action 7.4 we should use
                //JTidy or NekoHTML to parse the thlm


                DocumentFragment node = new HTMLDocumentImpl().createDocumentFragment();
                domParser.parse(new InputSource(new StringReader(note.getContent())), node);
                StringBuffer sb = new StringBuffer();
                sb.setLength(0);
                getText(sb, node);
                String text = sb.toString();

                if ((text != null) && (!text.equals(""))) {
                    //LOG.info("indexing "+text);
                    Field contentField = new Field("content",
                            text,
                            Field.Store.NO,
                            Field.Index.ANALYZED);
                    document.add(titleField);
                }


                writer.addDocument(document);
            }
            writer.commit();
            writer.optimize();
            writer.close();


        } catch (Exception ex) {
            LOG.log(Level.WARNING, "exception", ex);
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        LOG.info("Rebuild index finished");
    }

    private Collection<Note> getAllNotes() {
        NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);

        Collection<Note> toReturn = rep.getAllNotes();

        return toReturn;
    }

    private void getText(StringBuffer sb, Node node) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            sb.append(node.getNodeValue());
        }
        NodeList children = node.getChildNodes();
        if (children != null) {
            int len = children.getLength();
            for (int i = 0; i < len; i++) {
                getText(sb, children.item(i));
            }
        }
    }
}