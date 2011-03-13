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
import com.rubenlaguna.en4j.noterepository.NoteRepositoryChooser;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 *
 * @author ecerulm
 */
@ServiceProvider(service=NoteFinder.class)
public class NoteFinderLuceneImpl implements NoteFinder {

    private final int REPORTEVERY = 100;
    private static Logger LOG = Logger.getLogger(NoteFinderLuceneImpl.class.getName());
    private IndexReader reader = null;
    private final ThreadPoolExecutor RP = new ThreadPoolExecutor(1, 4, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(2), new MyThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
    private boolean pendingCommit = false;
    public static final int TIME_BETWEEN_COMMITS = 10000;
    private NoteRepository nr = null;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final RequestProcessor.Task COMMITER = new RequestProcessor().create(new Runnable() {

        public void run() {
            commitToIndex();
        }
    });
    private long lastRun = 0;

    public NoteFinderLuceneImpl() {
        this(NoteRepositoryChooser.getDefault());
    }

    NoteFinderLuceneImpl(NoteRepository testNoteRepository) {
        nr = testNoteRepository;
        try {
            //make sure that there is an index that the readers can open
            IndexWriterWrapper.getInstance().commit();
            Installer.mbean.setThreadPoolExecutor(RP);
            Directory dir = IndexWriterWrapper.getInstance().getLuceneDirectory();
            reader = IndexReader.open(dir, true);
        } catch (CorruptIndexException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void fireChange() {
        this.pcs.firePropertyChange("index", true, false);
    }

    private String getTextFromInputStream(ByteArrayInputStream byteArrayInputStream) {
        try {
            return getTextFromInputSource(new InputSource(byteArrayInputStream));
        } catch (SAXException ex) {
            LOG.log(Level.INFO, "caught exception: ", ex);
        } catch (IOException ex) {
            LOG.log(Level.INFO, "caught exception: ", ex);
        } finally {
            try {
                byteArrayInputStream.close();
            } catch (IOException ex) {
                LOG.log(Level.INFO, "caught exception: ", ex);
            }
        }
        return "";
    }

    private String getTextFromNoteContent(Note note) {
        final Reader contentAsReader = note.getContentAsReader();
        InputSource is = new InputSource(contentAsReader);
        try {
            return getTextFromInputSource(is);
        } catch (SAXException ex) {
            LOG.log(Level.INFO, "caught exception: ", ex);
        } catch (IOException ex) {
            LOG.log(Level.INFO, "caught exception: ", ex);
        } finally {
            try {
                contentAsReader.close();
            } catch (IOException ex) {
                LOG.log(Level.INFO, "caught exception: ", ex);
            }
        }
        return "";
    }

    private String getTextFromInputSource(InputSource is) throws SAXException, IOException {
        XMLReader xr = new org.ccil.cowan.tagsoup.Parser();
        ContentHandler handler = new MyHandler();
        xr.setContentHandler(handler);
        xr.setErrorHandler(new ErrorHandler() {

            public void warning(SAXParseException exception) throws SAXException {
                LOG.log(Level.WARNING, "exception while parsing note contents", exception);
            }

            public void error(SAXParseException exception) throws SAXException {
                LOG.log(Level.WARNING, "exception while parsing note contents", exception);
            }

            public void fatalError(SAXParseException exception) throws SAXException {
                LOG.log(Level.WARNING, "exception while parsing note contents", exception);
            }
        });
        xr.parse(is);
        String text = handler.toString();
        return text;
    }

    public void setInfoStream(PrintStream os) {
        IndexWriterWrapper.getInstance().setInfoStream(os);
    }

    public int getNumDocs() {
        try {
            return IndexWriterWrapper.getInstance().numDocs();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return 0;
    }

    @SuppressWarnings("SleepWhileHoldingLock")
    public void commitToIndex() {
        try {
            if (pendingCommit) {
                while ((System.currentTimeMillis() - lastRun) < TIME_BETWEEN_COMMITS) {
                    long x = TIME_BETWEEN_COMMITS - (System.currentTimeMillis() - lastRun);
                    LOG.log(Level.INFO, "waiting {0} ms before committing changes to index.", x);
                    Thread.sleep(x);
                }
                long previousRun = lastRun;
                lastRun = System.currentTimeMillis();
                pendingCommit = false;
                LOG.log(Level.INFO, "committing lucene index now. ({0}) {1} secs from previous run", new Object[]{lastRun, (lastRun - previousRun) / 1000});
                IndexWriterWrapper.getInstance().commit();
                fireChange();
                COMMITER.schedule(TIME_BETWEEN_COMMITS);
            } else {
                LOG.info("skipping commit. Nothing to commit to the index");
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "exception while commiting changes to the index", ex.getMessage());
            LOG.log(Level.FINE, "exception while commiting changes to the index", ex);
        }
    }

    public Collection<Note> find(String searchText) {
        if ("".equals(searchText.trim())) {
            return Collections.EMPTY_LIST;
        }
        long start = System.currentTimeMillis();
        searchText = searchText.trim();
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
                reader.close();
            }
            reader = newReader;
            LOG.info("using index version: " + reader.getVersion());
            final IndexSearcher searcher = new IndexSearcher(reader);

            final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_29);
            QueryParser parser = new CustomQueryParser("all", analyzer);
            parser.setDefaultOperator(QueryParser.Operator.AND);

            Query query = parser.parse(searchText);
            LOG.info("query =" + query.toString());
            //search the query
            Collector collector = new Collector() {

                private int docBase = 0;

                @Override
                public void setScorer(Scorer scorer) throws IOException {
                }

                @Override
                public void collect(int doc) throws IOException {
                    int scoreId = doc + docBase;
                    Document document = searcher.doc(scoreId);
                    final String stringValue = document.getField("id").stringValue();
                    int docId = Integer.parseInt(stringValue);
                    LOG.fine("doc id " + stringValue + " matches the search.");
                    toReturn.add(nr.get(docId, false));
                }

                @Override
                public void setNextReader(IndexReader reader, int docBase) throws IOException {
                    this.docBase = docBase;
                }

                @Override
                public boolean acceptsDocsOutOfOrder() {
                    return true;
                }
            };
            searcher.search(query, collector);
            searcher.close();
        } catch (ParseException ex) {
            Exceptions.printStackTrace(ex);
        } catch (CorruptIndexException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalStateException ex) {
            LOG.info("caught " + ex.getMessage() + ". Most likely the app is shutting down");
        }
        long delta = System.currentTimeMillis() - start;
        Installer.mbean.sampleSearchTime(delta);
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
                        if (note.isActive()) {
                            document = getLuceneDocument(note);
                            IndexWriterWrapper.getInstance().updateDocument(new Term("id", n.getId().toString()), document);
                            if (!pendingCommit) {
                                pendingCommit = true;
                                LOG.info("scheduling COMMITER");
                                COMMITER.schedule(TIME_BETWEEN_COMMITS);
                            }
                        } else {
                            LOG.log(Level.INFO, "delete note (id:{0}) from index", n.getId());
                            remove(n);
                        }
                    } catch (IllegalStateException ex) {
                        //indexer is closed. probably the whole app is closing
                        return;
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "couldn't index note " + note.getTitle(), ex);
                        return;
                    } finally {
                        if (document != null) {
                            for (Fieldable field : document.getFields()) {
                                final Reader reader = field.readerValue();
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException ex) {
                                    }
                                }
                            }
                        }
                        //TODO close readers in document
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
        IndexWriterWrapper writer = null;
        try {
            writer = IndexWriterWrapper.getInstance();
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
                        if (ph != null) {
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
            while (RP.getActiveCount() > 0 || RP.getQueue().size() > 0) {
                LOG.finer("thread sleep until all notes are really indexed");
                Thread.sleep(1000);
            }
            commitToIndex();
            LOG.info(i + " notes indexed so far.");
            LOG.info("Optimize and close the index.");
            start2 = System.currentTimeMillis();
            writer.optimize();
            //commitToIndex();
            fireChange();
            long delta = System.currentTimeMillis() - start2;
            LOG.info("Index optimized.It took " + delta / 1000.0 + " secs.");
        } catch (IllegalStateException ex) {
            LOG.info("rebuild index failed. is the app closing?");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "exception", ex.getMessage());
            LOG.log(Level.FINE, "exception", ex);
        }
        long delta = System.currentTimeMillis() - start;
        LOG.info("Rebuild index finished. It took " + delta / 1000L + " secs.");
    }

    private Document getLuceneDocument(Note note) throws SAXException, IOException {
        //Lucene document http://www.darksleep.com/lucene/
        Document document = new Document();
        Field idField = new Field("id", note.getId().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED);
        document.add(idField);

        if (note.getGuid() != null) {
            Field guidField = new Field("guid", note.getGuid(), Field.Store.YES, Field.Index.NOT_ANALYZED);
            document.add(guidField);
        }

        if (note.getTitle() != null) {
            Field titleField = new Field("title", note.getTitle(), Field.Store.YES, Field.Index.ANALYZED);
            document.add(titleField);
        }

        String text = getTextFromNoteContent(note);
        LOG.log(Level.FINE, "note text: {0}", text);
        if ((text != null) && (!text.equals(""))) {
            Field contentField = new Field("content", text, Field.Store.NO, Field.Index.ANALYZED);
            document.add(contentField);
        }

        String sourceUrl = note.getSourceurl();
        if ((null != sourceUrl) && (!"".equals(sourceUrl))) {
            Field sourceField = new Field("source", sourceUrl, Field.Store.NO, Field.Index.ANALYZED);
            document.add(sourceField);
        }
        StringBuilder allText = new StringBuilder();
        allText.append(note.getTitle());
        allText.append(" ").append(text);
        allText.append(" ").append(sourceUrl);
        for (Resource r : note.getResources()) {
            if (r == null) {
                LOG.warning("How come getResources returns some null resources?");
                continue;
            }
            LOG.log(Level.FINE, "resource: {0} type: {1} from note: {2}", new Object[]{r.getFilename(), r.getMime(), note.getTitle()});
            if (r.getRecognition() != null) {
                LOG.log(Level.FINE,"recognition is not null for " + "resource: " + "{0} type: {1} from note: {2}", new Object[]{r.getFilename(), r.getMime(), note.getTitle()});
                final String recognitionText = getTextFromInputStream(new ByteArrayInputStream(r.getRecognition()));
                LOG.log(Level.FINE, "recognitionText: {0}", recognitionText);
                allText.append(" ").append(recognitionText);
            } else {

                if (r.getMime() != null && r.getMime().contains("image")) {
                    LOG.log(Level.FINE,"no recognition for " + "resource: " + "{0} type: {1} from note: {2}", new Object[]{r.getFilename(), r.getMime(), note.getTitle()});
                }
            }
            if (isDocument(r)) {
                Metadata metadata = new Metadata();
                if (r.getFilename() != null) {
                    metadata.set(Metadata.RESOURCE_NAME_KEY, r.getFilename());
                }
                metadata.set(Metadata.CONTENT_TYPE, r.getMime());
                try {
                    final InputStream dataAsInputStream = r.getDataAsInputStream();
                    final Reader docTikaReader = new Tika().parse(dataAsInputStream, metadata);
                    final Reader resourceReader = new ReaderThatEatsUpExceptions(docTikaReader, dataAsInputStream);
                    document.add(new Field("all", resourceReader));

                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "couldn''t parse resource ({0}) in note ({1}) TikaException catched", new Object[]{r.getMime(), note.getTitle()});
                }
            }
        }

        Field allField = new Field("all", allText.toString().trim(), Field.Store.NO, Field.Index.ANALYZED);
        LOG.log(Level.FINEST, "note indexed with text:\n{0}", allText.toString());
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

    private Collection<Note> getAllNotes() {
        Collection<Note> toReturn = nr.getAllNotes();
        return toReturn;
    }

    private Note getProperNote(Note noteWithoutContents) {
        final Integer id = noteWithoutContents.getId();
        final Note noteFromDatabase = nr.get(id);
        if (noteFromDatabase == null){
            return null;
        }
        if (noteFromDatabase.getGuid() == null || noteFromDatabase.getTitle() == null) {
            LOG.log(Level.WARNING, "How come entry (id:{0}) entry has no guid?", id);
            //better return null than some corrupted entry
            return null;
        }

        return noteFromDatabase;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    public void remove(Note n) {
        removeById(n.getId());
        removeByGuid(n.getGuid());
    }

    public void removeByGuid(final String noteguid) {
        if (noteguid == null) {
            return;
        }
        RP.submit(new Runnable() {

            public void run() {
                try {
                    LOG.log(Level.INFO, "removing from index guid:{0}", noteguid);
                    IndexWriterWrapper.getInstance().deleteDocuments(new Term("guid", noteguid));
                    if (!pendingCommit) {
                        pendingCommit = true;
                        LOG.info("scheduling COMMITER");
                        COMMITER.schedule(TIME_BETWEEN_COMMITS);
                    }
                } catch (IOException ex) {
                    LOG.log(Level.WARNING, "caught exception:", ex);
                }
            }
        });
    }

    public void removeById(final int id) {
        RP.submit(new Runnable() {

            public void run() {
                try {
                    LOG.log(Level.INFO, "removing from index id:{0}", id);
                    IndexWriterWrapper.getInstance().deleteDocuments(new Term("id", Integer.toString(id)));
                    if (!pendingCommit) {
                        pendingCommit = true;
                        LOG.info("scheduling COMMITER");
                        COMMITER.schedule(TIME_BETWEEN_COMMITS);
                    }
                } catch (IOException ex) {
                    LOG.log(Level.WARNING, "caught exception:", ex);
                }
            }
        });
    }

    public void close() {
        RP.shutdownNow();
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
