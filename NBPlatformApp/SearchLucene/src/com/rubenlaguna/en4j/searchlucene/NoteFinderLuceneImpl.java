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
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.interfaces.NoteFinder;
import com.rubenlaguna.en4j.interfaces.NoteRepository;

import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author ecerulm
 */
public class NoteFinderLuceneImpl implements NoteFinder {

    private final int REPORTEVERY = 100;
    private static Logger LOG = Logger.getLogger(NoteFinderLuceneImpl.class.getName());
    private IndexReader reader = null;
    private final ThreadPoolExecutor RP = new ThreadPoolExecutor(1, 4, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(2), new MyThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
    private boolean pendingCommit = false;
    public static final int TIME_BETWEEN_COMMITS = 10000;
    private NoteRepository nr = null;
    private final RequestProcessor.Task COMMITER = RequestProcessor.getDefault().create(new Runnable() {

        public void run() {
            commitToIndex();
        }
    });
    private long lastRun = 0;

    public NoteFinderLuceneImpl() {
        this(Lookup.getDefault().lookup(NoteRepository.class));
    }

    NoteFinderLuceneImpl(NoteRepository testNoteRepository) {
        nr = testNoteRepository;
        try {
            //make sure that there is an index that the readers can open
            IndexWriterFactory.getIndexWriter().commit();
            Installer.mbean.setThreadPoolExecutor(RP);
            File file = new File(System.getProperty("netbeans.user") + "/en4jluceneindex");
            reader = IndexReader.open(FSDirectory.open(file), true);
        } catch (CorruptIndexException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void setInfoStream(PrintStream os) {
        IndexWriterFactory.getIndexWriter().setInfoStream(os);
    }

    public int getNumDocs() {
        try {
            return IndexWriterFactory.getIndexWriter().numDocs()-IndexWriterFactory.getIndexWriter().numRamDocs();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return 0;
    }

    public void commitToIndex() {
        try {
            if (pendingCommit) {
                while ((System.currentTimeMillis() - lastRun) < TIME_BETWEEN_COMMITS) {
                    long x = TIME_BETWEEN_COMMITS - (System.currentTimeMillis() - lastRun);
                    LOG.info("waiting " + x + " ms before committing changes to index.");
                    Thread.sleep(x);
                }
                long previousRun = lastRun;
                lastRun = System.currentTimeMillis();
                pendingCommit = false;
                LOG.info("committing lucene index now. (" + lastRun + ") " + (lastRun - previousRun) / 1000 + " secs from previous run");
                IndexWriterFactory.getIndexWriter().commit();
                COMMITER.schedule(TIME_BETWEEN_COMMITS);
            } else {
                LOG.info("skipping commit. Nothing to commit to the index");
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "exception while commiting changes to the index", ex);
        }
    }

    public Collection<Note> find(String searchText) {
        if ("".equals(searchText.trim())) {
            return Collections.EMPTY_LIST;
        }
        long start = System.currentTimeMillis();
        searchText = searchText.trim();
//        String patternStr = "(?:\\w)\\s+";
        String patternStr = "\\s+";
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
            IndexReader newReader = reader.reopen();
            if (newReader != reader) {
                LOG.info("reader reopened");
                reader.close();
            }
            reader = newReader;

            reader.reopen();
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

                    //NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
                    toReturn.add(nr.get(docId, false));
                }

                @Override
                public void setNextReader(IndexReader reader, int docBase) throws IOException {
                }

                @Override
                public boolean acceptsDocsOutOfOrder() {
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
        long delta = System.currentTimeMillis() - start;
        LOG.info("find took " + delta / 1000.0 + " secs. " + toReturn.size() + " results found");
        return toReturn;
    }

    public void index(final Note n) {
        LOG.fine("submitting note (" + n.getTitle() + ") for indexing");
        RP.submit(new Runnable() {

            public void run() {
                LOG.fine("Generating a lucene document from note (" + n.getTitle() + ")");
                Note note = getProperNote(n);
                Document document = null;
                if (null != note) {
                    try {
                        document = getLuceneDocument(note);
                        IndexWriterFactory.getIndexWriter().addDocument(document);
                        if (!pendingCommit) {
                            pendingCommit = true;
                            LOG.info("scheduling COMMITER");
                            COMMITER.schedule(TIME_BETWEEN_COMMITS);
                        }
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "couldn't index note " + note.getGuid(), ex);
                        return;
                    }
                } else {
                    return;
                }
            }
        });

    }

    public synchronized void rebuildIndex(ProgressHandle ph) {
        LOG.info("about to start indexing");
        long start = System.currentTimeMillis();
        long start2 = start;
        IndexWriter writer = null;
        try {
            writer = IndexWriterFactory.getIndexWriter();
            writer.deleteAll();
            writer.commit();
            Collection<Note> notes = getAllNotes();

            LOG.info("number of notes " + notes.size());
            if (null != ph) {
                ph.switchToDeterminate(notes.size());
            }

            int i = 0;
            for (Note noteWithoutContents : notes) {
                if (Thread.interrupted()) {
                    LOG.warning("Rebuild index operation was CANCELLED");
                    return;
                }
                Note note = getProperNote(noteWithoutContents);

                if (null != note) {
                    index(note);
                    ++i;
                    if ((i % REPORTEVERY) == 0) {
                        if (null != null) {
                            ph.progress("Note: " + note.getTitle(), i);
                        }
                        long delta = System.currentTimeMillis() - start2;
                        start2 = System.currentTimeMillis();
                        LOG.fine(i + " notes indexed so far. This batch took " + (delta / 1000.0) + " secs");
                    }
                } else {
                    break;//for loop
                }
            }
            while(RP.getActiveCount()>0 || RP.getQueue().size()>0) {
                LOG.finer("thread sleep until all notes are really indexed");
                Thread.sleep(1000);
            }
            writer.commit();
            LOG.info(i + " notes indexed so far.");
            LOG.info("Optimize and close the index.");
            start2 = System.currentTimeMillis();
            writer.optimize();
            long delta = System.currentTimeMillis() - start2;
            LOG.info("Index optimized and closed.It took " + delta / 1000.0 + " secs.");

        } catch (Exception ex) {
            LOG.log(Level.WARNING, "exception", ex);
            Exceptions.printStackTrace(ex);
        }
        long delta = System.currentTimeMillis() - start;
        LOG.info("Rebuild index finished. It took " + delta / 1000L + " secs.");
    }

    private Document getLuceneDocument(Note note) throws SAXException, IOException {
        //Lucene document http://www.darksleep.com/lucene/
        Document document = new Document();
        Field idField = new Field("id", note.getId().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED);
        document.add(idField);
        Field titleField = new Field("title", note.getTitle(), Field.Store.YES, Field.Index.ANALYZED);
        document.add(titleField);
        DocumentFragment node = parseNote(note);
        //StringBuffer sb = new StringBuffer();
        //sb.append(getText(node));
        String text = getText(node);
        if ((text != null) && (!text.equals(""))) {
            //LOG.info("indexing "+text);
            Field contentField = new Field("content", text, Field.Store.NO, Field.Index.ANALYZED);
            document.add(contentField);
        }
        String sourceUrl = note.getSourceurl();
        if ((null != sourceUrl) && (!"".equals(sourceUrl))) {
            //                    LOG.info("sourceUrl: \"" + sourceUrl + "\"");
            Field sourceField = new Field("source", sourceUrl, Field.Store.NO, Field.Index.ANALYZED);
            document.add(sourceField);
        }
        StringBuffer allText = new StringBuffer();
        allText.append(note.getTitle());
        allText.append(" ").append(text);
        allText.append(" ").append(sourceUrl);
        for (Resource r : note.getResources()) {
            LOG.fine("resource: " + r.getFilename() + " type: " + r.getMime() + " from note: " + note.getTitle());
            if (r.getRecognition() != null) {
                LOG.fine("recognition is not null for " + "resource: " + r.getFilename() + " type: " + r.getMime() + " from note: " + note.getTitle());
                DocumentFragment rnode = parseXmlByteArray(r.getRecognition());
                final String recognitionText = getText(rnode);
                LOG.fine("recognitionText: " + recognitionText);
                allText.append(" ").append(recognitionText);
            } else {
                if (r.getMime().contains("image")) {
                    LOG.fine("no recognition for " + "resource: " + r.getFilename() + " type: " + r.getMime() + " from note: " + note.getTitle());
                }
            }
            if (isDocument(r)) {
                Metadata metadata = new Metadata();
                if (r.getFilename() != null) {
                    metadata.set(Metadata.RESOURCE_NAME_KEY, r.getFilename());
                }
                metadata.set(Metadata.CONTENT_TYPE, r.getMime());
                try {
                    final String parseResourceText = new Tika().parseToString(new ByteArrayInputStream(r.getData()), metadata);
                    if (!"".equals(parseResourceText)) {
                        LOG.fine("resource: " + r.getFilename() + " type: " + r.getMime() + " from note: " + note.getTitle() + "\n parseResourceText (" + r.getMime() + "): " + parseResourceText.substring(0, Math.min(200, parseResourceText.length())).trim());
                    }
                    allText.append(parseResourceText);
                } catch (TikaException ex) {
                    LOG.log(Level.WARNING, "couldn't parse resource (" + r.getMime() + ") TikaException catched", ex);
                }
            }
        }

        Field allField = new Field("all", allText.toString().trim(), Field.Store.NO, Field.Index.ANALYZED);
        LOG.finest("note indexed with text:\n" + allText.toString());
        document.add(allField);
        return document;
    }

    private boolean isDocument(Resource r) {
        boolean isDocument = true;
        isDocument = isDocument && !"application/vnd.evernote.ink".equals(r.getMime());
        if (null != r.getMime()) {
            isDocument = isDocument && !r.getMime().contains("image");
        }
        return isDocument;
    }

    private DocumentFragment parseNote(Note note) throws IOException, SAXException {
        //according to Lucene in Action 7.4 we should use
        //JTidy or NekoHTML to parse the thlm
        DocumentFragment node = new HTMLDocumentImpl().createDocumentFragment();
        DOMFragmentParser domParser = new DOMFragmentParser();
        domParser.parse(new InputSource(new StringReader(note.getContent())), node);
        return node;
    }

    private Collection<Note> getAllNotes() {
//        NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
        Collection<Note> toReturn = nr.getAllNotes();
        return toReturn;
    }

    private String getText(Node node) {
        final StringBuffer sb = new StringBuffer(" ");
        final String localName = node.getNodeName();
        if ("en-media".equalsIgnoreCase(localName)) {
            final String fname = ((Element) node).getAttribute("alt");
            if (null != fname) {
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
                sb.append(getText(children.item(i)));
            }
        }
        return sb.toString();
    }

    private Note getProperNote(Note noteWithoutContents) {
//        final NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
        final Integer id = noteWithoutContents.getId();
        return nr.get(id);
    }

    private DocumentFragment parseXmlByteArray(byte[] theArray) throws SAXException, IOException {
        DocumentFragment node = new HTMLDocumentImpl().createDocumentFragment();
        DOMFragmentParser domParser = new DOMFragmentParser();
        domParser.parse(new InputSource(new ByteArrayInputStream(theArray)), node);
        return node;
    }
}

class MyThreadFactory implements ThreadFactory {

    private final ThreadFactory factory = Executors.defaultThreadFactory();

    public Thread newThread(Runnable r) {
        Thread toReturn = factory.newThread(r);
        toReturn.setName("indexing " + toReturn.getName());
        return toReturn;
    }
}
