/*
 * SearchLuceneMBeanImpl.java
 *
 * Created on March 2, 2010, 7:48 PM
 */
package com.rubenlaguna.en4j.searchlucene;

import java.util.Queue;
import org.apache.lucene.document.Document;

/**
 * Class SearchLuceneMBeanImpl
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public class SearchLuceneMBeanImpl implements SearchLuceneMBeanImplMBean {

    private Queue<Document> theQueue;

    public SearchLuceneMBeanImpl() {
    }

    /**
     * Get number of documents waiting to be indexed
     */
    public int getIndexQueueSize() {
        if (null == theQueue) {
            return 0;
        }
        return theQueue.size();
    }

    void setQueue(Queue<Document> theQueue) {
        this.theQueue=theQueue;
    }
}


