/*
 * SearchLuceneMBeanImpl.java
 *
 * Created on March 2, 2010, 7:48 PM
 */
package com.rubenlaguna.en4j.searchlucene;

import java.util.Queue;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.lucene.document.Document;

/**
 * Class SearchLuceneMBeanImpl
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public class SearchLuceneMBeanImpl implements SearchLuceneMBeanImplMBean {

    private ThreadPoolExecutor threadPoolExecutor;

    public SearchLuceneMBeanImpl() {
    }

    /**
     * Get number of documents waiting to be indexed
     */
    public int getIndexQueueSize() {
        if (null == threadPoolExecutor) {
            return 0;
        }
        return threadPoolExecutor.getQueue().size();
    }

    /**
     * Get number of actived threads
     */
    public int getActiveThreads() {
        if (null == threadPoolExecutor) {
            return 0;
        }
        return threadPoolExecutor.getActiveCount();
    }

    void setThreadPoolExecutor(ThreadPoolExecutor RP) {
        this.threadPoolExecutor = RP;
    }
}


