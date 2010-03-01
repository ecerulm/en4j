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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.netbeans.api.progress.ProgressHandle;
import org.omg.CosNaming.NamingContextPackage.NotEmpty;
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
    //private DOMFragmentParser domParser = new DOMFragmentParser();
    private static Logger LOG = Logger.getLogger(NoteFinderLuceneImpl.class.getName());
    private IndexReader reader = null;
    private final RequestProcessor RP = new RequestProcessor("Indexing thread", 1, true);
    private final BlockingQueue<Document> theQueue = new ArrayBlockingQueue<Document>(100, false);
    private final IndexWriterMonitor iwm = new IndexWriterMonitor();
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    //private IndexWriter theWriter;
    public NoteFinderLuceneImpl() {
        try {
            //getIndexWriter().close();
            iwm.getIndexWriter();
            iwm.release();
            File file = new File(System.getProperty("netbeans.user") + "/en4jluceneindex");
            reader = IndexReader.open(FSDirectory.open(file), true);
        } catch (CorruptIndexException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        RP.post(new IndexerTask(iwm, theQueue,lock,notEmpty));
    }
    //private Analyzer analyzer = new SimpleAnalyzer();

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

                    NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
                    toReturn.add(rep.get(docId, false));
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
        long delta = System.currentTimeMillis() - start;
        LOG.info("find took " + delta / 1000.0 + " secs. " + toReturn.size() + " results found");
        return toReturn;
    }

    public void index(Note n) {
        Note note = getProperNote(n);
        Document document = null;
        if (null != note) {
            try {
                document = getLuceneDocument(note);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "couldn't parse note " + note.getGuid(), ex);
                return;
            }
        } else {
            return;
        }

        LOG.info("Trying to add note " + note + " to the indexing queue");
        try {
            //we have a document
            theQueue.put(document);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
        LOG.info("Note " + note + "was added in the indexing queue");
        lock.lock();
        notEmpty.signalAll();
        lock.unlock();
    }

    public synchronized void rebuildIndex(ProgressHandle ph) {
        LOG.info("about to start indexing");
        long start = System.currentTimeMillis();
        long start2 = start;
        lock.lock();
        IndexWriter writer = null;
        try {
            writer = iwm.getIndexWriter();
            writer.deleteAll();
            writer.commit();
            Collection<Note> notes = getAllNotes();

            LOG.info("number of notes " + notes.size());
            ph.switchToDeterminate(notes.size());

            int i = 0;
            for (Note noteWithoutContents : notes) {
                if (Thread.interrupted()) {
                    //if the task has been cancelled we skip the rest of the
                    //notes but we still do the writer.commit()
                    writer.close();
                    LOG.warning("Rebuild index operation was CANCELLED");
                    return;
                }
                Note note = getProperNote(noteWithoutContents);

//                LOG.info("indexing note " + note);

                if (null != note) {
                    Document document = getLuceneDocument(note);
                    writer.addDocument(document);
                    ++i;
                    if ((i % REPORTEVERY) == 0) {
//                    if ((System.currentTimeMillis() - start2) > 2000) { //every 5 secs
                        //to process 12000 notes
                        //without commiting/optimizing every 100th   137 secs
                        //with    commiting only       every 100th   144 secs
                        //with    committin only each 5s             201 sec
                        //with    committin only each 2s             332 sec
                        //with    commiting/optimizing each          502 secs
                        //with    comminting 5s and progress outside 410 sec

                        ph.progress("Note: " + note.getTitle(), i);
                        writer.commit();
                        long delta = System.currentTimeMillis() - start2;
                        start2 = System.currentTimeMillis();
                        LOG.fine(i + " notes indexed so far. This batch took " + (delta / 1000.0) + " secs");
                    }
                } else {
                    break;//for loop
                }
            }
            writer.commit();
            LOG.info(i + " notes indexed so far.");
            LOG.info("Optimize and close the index.");
            start2 = System.currentTimeMillis();
            writer.optimize();
            writer.close();
            long delta = System.currentTimeMillis() - start2;
            LOG.info("Index optimized and closed.It took " + delta / 1000.0 + " secs.");

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
            iwm.release();
            lock.unlock();
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
        //according to Lucene in Action 7.4 we should use
        //JTidy or NekoHTML to parse the thlm
        DocumentFragment node = new HTMLDocumentImpl().createDocumentFragment();
        DOMFragmentParser domParser = new DOMFragmentParser();
        domParser.parse(new InputSource(new StringReader(note.getContent())), node);
        StringBuffer sb = new StringBuffer();
        sb.setLength(0);
        getText(sb, node);
        String text = sb.toString();
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
        Field allField = new Field("all", allText.toString().trim(), Field.Store.NO, Field.Index.ANALYZED);
        document.add(allField);
        return document;
    }

    private Collection<Note> getAllNotes() {
        NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);

        Collection<Note> toReturn = rep.getAllNotes();

        return toReturn;
    }

    private void getText(StringBuffer sb, Node node) {
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
                getText(sb, children.item(i));
            }
        }
    }

    private Note getProperNote(Note noteWithoutContents) {
        return Lookup.getDefault().lookup(NoteRepository.class).get(noteWithoutContents.getId());

    }
}

class IndexWriterMonitor {

    private final Semaphore sem = new Semaphore(1);
    private IndexWriter current = null;

    public synchronized IndexWriter getIndexWriter() {
        try {
            sem.acquire();
            if (current == null) {
                File file = new File(System.getProperty("netbeans.user") + "/en4jluceneindex");
                final CustomAnalyzer analyzer = new CustomAnalyzer();
                IndexWriter writer = new IndexWriter(FSDirectory.open(file), analyzer, IndexWriter.MaxFieldLength.LIMITED);
                writer.setUseCompoundFile(true);
                current = writer;
            }
            return current;
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    public synchronized void release() {
        if (null != current) {
            try {
                current.close();
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                current = null;
            }
        }
        sem.release();
    }
}

class IndexerTask implements Runnable {

    private static final Logger LOG = Logger.getLogger(IndexerTask.class.getName());
    private final IndexWriterMonitor iwm;
    private final BlockingQueue<Document> theQueue;
    private final Lock lock;
    private final Condition notEmpty;

    IndexerTask(IndexWriterMonitor iwm,BlockingQueue<Document> theQueue,Lock lock,Condition notEmpty) {
        this.iwm = iwm;
        this.theQueue = theQueue;
        this.lock = lock;
        this.notEmpty = notEmpty;
    }

    public void run() {
        int i = 0;
        IndexWriter writer = null;
        boolean noerrors = true;
        writer = iwm.getIndexWriter();
        while (noerrors && (!Thread.currentThread().isInterrupted())) {
            i++;
            LOG.info("before acquiring lock in indexer thread");
            try {
                while (theQueue.isEmpty()) {
                    LOG.info("indexing queue is empty. waiting...");
                    iwm.release();
                    writer = null;
                    lock.lock();

                    try {
                        notEmpty.await();
                    } catch (InterruptedException interruptedException) {
                    } finally {
                        lock.unlock();
                    }
                    LOG.info("indexing queue notEmpty condition fulfilled");
                    writer = iwm.getIndexWriter();
                }
                LOG.info("indexing queue size: " + theQueue.size());
                Document document = theQueue.poll();
                if (null != document) {
                    writer.addDocument(document);
                    LOG.info("Indexed note " + document.getField("title").stringValue());
                    writer.commit();
                }
            } catch (Exception ex) {
                noerrors = false;
                Exceptions.printStackTrace(ex);
            }
            LOG.info("continue while loop");
        } //while
        try {
            if (writer != null) {
                iwm.release();
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        LOG.warning("indexer thread terminated!!");
    }
}
