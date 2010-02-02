/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.interfaces.NoteFinder;
import com.rubenlaguna.en4j.interfaces.NoteRepository;

import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
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
    //private Analyzer analyzer = new SimpleAnalyzer();

    public Collection<Note> find(String searchText) {
        if ("".equals(searchText.trim())) {
            return Collections.EMPTY_LIST;
        }
        searchText = searchText.trim();
        String patternStr = "(?:\\w)\\s+";
        String replaceStr = "* ";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(searchText);
        searchText = matcher.replaceAll(replaceStr);
        if (Pattern.matches(".*\\w$", searchText)) {
            searchText = searchText + "*";
        }

        LOG.info("search text:" + searchText);
        final Collection<Note> toReturn = new ArrayList<Note>();

        try {
            File file = new File(System.getProperty("netbeans.user") + "en4j/luceneindex");
            IndexReader reader = IndexReader.open(FSDirectory.open(file), true);

            final IndexSearcher searcher = new IndexSearcher(reader);

            final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
            AnalyzerUtils.displayTokensWithFullDetails(analyzer, searchText);
            QueryParser parser = new CustomQueryParser("all", analyzer);
            parser.setDefaultOperator(QueryParser.Operator.AND);

            Query query = parser.parse(searchText);
            LOG.info("query =" + query.toString());
            //search the query
            Collector collector = new Collector() {

                @Override
                public void setScorer(Scorer scorer) throws IOException {
                    //throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void collect(int doc) throws IOException {
                    int scoreId = doc;
                    Document document = searcher.doc(scoreId);
                    int docId = Integer.parseInt(document.getField("id").stringValue());

                    NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
                    toReturn.add(rep.get(docId));
                }

                @Override
                public void setNextReader(IndexReader reader, int docBase) throws IOException {
                    //throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public boolean acceptsDocsOutOfOrder() {
                    //throw new UnsupportedOperationException("Not supported yet.");
                    return true;
                }
            };
            searcher.search(query, collector);


        } catch (ParseException ex) {
            Exceptions.printStackTrace(ex);
        } catch (CorruptIndexException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return toReturn;
    }

    public void rebuildIndex() {
        IndexWriter writer = null;
        LOG.info("about to start indexing");
        try {
            File file = new File(System.getProperty("netbeans.user") + "en4j/luceneindex");
            final CustomAnalyzer analyzer = new CustomAnalyzer();
            writer = new IndexWriter(FSDirectory.open(file), analyzer, true,
                    IndexWriter.MaxFieldLength.LIMITED);
            writer.setUseCompoundFile(true);
            writer.deleteAll();
            writer.commit();
            Collection<Note> notes = getAllNotes();

            LOG.info("number of notes " + notes.size());
            for (Note note : notes) {
                if (Thread.interrupted()) {
                    //if the task has been cancelled we skip the rest of the
                    //notes but we still do the writer.commit()
                    writer.close();
                    LOG.warning("Rebuild index operation was CANCELLED");
                    return;
                }

                LOG.info("indexing note " + note);


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
                    document.add(contentField);
                }

                //SourceURL
                String sourceUrl = note.getSourceurl();
                if (null != sourceUrl && !"".equals(sourceUrl));
                {
                    Field sourceField = new Field("source",
                            sourceUrl,
                            Field.Store.NO,
                            Field.Index.ANALYZED);
                    document.add(sourceField);
                }
                



                StringBuffer allText = new StringBuffer();
                allText.append(note.getTitle());
                allText.append(" ").append(text);
                allText.append(" ").append(sourceUrl);

                Field allField = new Field("all", allText.toString().trim(),
                        Field.Store.NO, Field.Index.ANALYZED);
                document.add(allField);
                //LOG.info("Indxing " + allText.toString());
                AnalyzerUtils.displayTokensWithFullDetails(analyzer, allText.toString());
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
        final String localName = node.getNodeName();
        if("en-media".equalsIgnoreCase(localName)){
            final String fname = ((Element)node).getAttribute("alt");
            if (null!=fname){
                sb.append(fname).append(" ");
            }
        }
        if (node.getNodeType() == Node.TEXT_NODE) {
            final String nodeValue = node.getNodeValue();
            //LOG.info("textnode " + nodeValue);
            sb.append(nodeValue);
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
