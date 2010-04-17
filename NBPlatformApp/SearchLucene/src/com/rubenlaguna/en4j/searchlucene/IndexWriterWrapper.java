/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.openide.util.Exceptions;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class IndexWriterWrapper {

    private static final Logger LOG = Logger.getLogger(IndexWriterWrapper.class.getName());
    private static boolean isClosed = false;
    private static File theDirectoryFile = null;
    private static IndexWriterWrapper theInstance = null;
    private final static int MAX_AVAILABLE = 5;
    private final Semaphore available = new Semaphore(MAX_AVAILABLE);
    private IndexWriter indexWriterInstance = null;
    private int commits = 0;

    private IndexWriterWrapper() {
        initWriter();
    }

    void initWriter() {

        if (indexWriterInstance == null) {
            try {
                File file = getDirectoryFile();
                final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_29);
                final FSDirectory theDir = FSDirectory.open(file);

                indexWriterInstance = new IndexWriter(theDir, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
                indexWriterInstance.setUseCompoundFile(true);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    void close() {
        available.acquireUninterruptibly(MAX_AVAILABLE);
        isClosed = true;
        if (indexWriterInstance != null) {
            LOG.info("IndexWriter closed!");
            try {
                indexWriterInstance.commit();
                indexWriterInstance.close();
            } catch (CorruptIndexException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                indexWriterInstance = null;
            }
        }
        available.release(MAX_AVAILABLE);
    }

    void updateDocument(Term term, Document document) throws CorruptIndexException, IOException {
        available.acquireUninterruptibly();
        try {
            indexWriterInstance.updateDocument(term, document);
        } finally {
            available.release();
        }
    }

    void deleteAll() throws IOException {
        available.acquireUninterruptibly();
        try {
            indexWriterInstance.deleteAll();
        } finally {
            available.release();
        }
    }

    void deleteDocuments(Term term) throws IOException {
        available.acquireUninterruptibly();
        try {
            indexWriterInstance.deleteDocuments(term);
        } finally {
            available.release();
        }
    }

    void commit() throws CorruptIndexException, IOException {
        available.acquireUninterruptibly();
        try {
            indexWriterInstance.commit();
            commits++;
        } finally {
            available.release();
        }
//        if (commits > 5) {
//            closeAndReopen();
//        }

    }

//    private void closeAndReopen() throws IOException {
//        available.acquireUninterruptibly(MAX_AVAILABLE);
//        LOG.info("the actual IndexWriter instance has been closed and replaced with a new instance");
//        indexWriterInstance.close();
//        indexWriterInstance = null;
//        commits = 0;
//        initWriter();
//        available.release(MAX_AVAILABLE);
//    }
    void optimize()
            throws CorruptIndexException, IOException {
        available.acquireUninterruptibly();
        try {
            indexWriterInstance.optimize();
        } finally {
            available.release();
        }
//        closeAndReopen();
    }

    void setInfoStream(PrintStream os) {
        available.acquireUninterruptibly();
        try {
            indexWriterInstance.setInfoStream(os);
        } finally {
            available.release();
        }
    }

    int numDocs() throws IOException {
        available.acquireUninterruptibly();
        try {
            return indexWriterInstance.numDocs();
        } finally {
            available.release();
        }
    }

    Directory getLuceneDirectory() {
        available.acquireUninterruptibly();
        try {
            return indexWriterInstance.getDirectory();
        } finally {
            available.release();
        }
    }

    static synchronized IndexWriterWrapper getInstance() {
        if (isClosed) {
            throw new IllegalStateException("Already closed.");
        }

        if (theInstance == null) {
            theInstance = new IndexWriterWrapper();
        }
        return theInstance;
    }

    private static File getDirectoryFile() throws IOException {
        if (theDirectoryFile == null) {
            String dir = System.getProperty("netbeans.user");
            if (dir == null) {
                File tempFile = File.createTempFile("en4j", "");
                tempFile.delete();
                tempFile.mkdir();
                dir = tempFile.getAbsolutePath();
            }
            theDirectoryFile = new File(dir + "/en4jluceneindex");
        }
        return theDirectoryFile;
    }
}
