/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.util.Version;
import org.openide.util.Exceptions;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class IndexWriterFactory {

    private static final Logger LOG = Logger.getLogger(IndexWriterFactory.class.getName());
    private static IndexWriter theInstance = null;
    private static boolean isClosed = false;
    private static File theDirectory=null;

    static synchronized IndexWriter getIndexWriter() {
        if (isClosed) {
            throw new IllegalStateException("Already closed.");
        }
        if (theInstance == null) {
            try {
                File file = getDirectory();
                final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_29);
                final FSDirectory theDir = FSDirectory.open(file);

                theInstance = new IndexWriter( theDir, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
                theInstance.setUseCompoundFile(true);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return null;
            }

        }
        return theInstance;
    }

    public static File getDirectory() throws IOException {
        if (theDirectory == null) {
            String dir = System.getProperty("netbeans.user");
            if (dir == null) {
                File tempFile = File.createTempFile("en4j", "");
                tempFile.delete();
                tempFile.mkdir();
                dir = tempFile.getAbsolutePath();
            }
            theDirectory = new File(dir + "/en4jluceneindex");
        }
        return theDirectory;
    }

    static synchronized void close() {
        isClosed = true;
        if (theInstance != null) {
            LOG.info("IndexWriter closed!");
            try {
                theInstance.commit();
                theInstance.close();
            } catch (CorruptIndexException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                theInstance = null;
            }
        }
    }
}
