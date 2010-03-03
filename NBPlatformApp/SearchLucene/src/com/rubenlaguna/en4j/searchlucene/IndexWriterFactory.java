/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.openide.util.Exceptions;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class IndexWriterFactory {

    private static final Logger LOG = Logger.getLogger(IndexWriterFactory.class.getName());
    private static IndexWriter theInstance = null;
    private static boolean isClosed = false;

    static synchronized IndexWriter getIndexWriter() {
        if (isClosed) {
            throw new IllegalStateException("Already closed.");
        }
        if (theInstance == null) {
            try {
                File file = new File(System.getProperty("netbeans.user") + "/en4jluceneindex");
                final CustomAnalyzer analyzer = new CustomAnalyzer();
                theInstance = new IndexWriter(FSDirectory.open(file), analyzer, IndexWriter.MaxFieldLength.LIMITED);
                theInstance.setUseCompoundFile(true);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return null;
            }

        }
        return theInstance;
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
