/*
 * SynchronizationMBeanImplMBean.java
 *
 * Created on March 2, 2010, 8:57 PM
 */
package com.rubenlaguna.en4j.sync;

/**
 * Interface SynchronizationMBeanImplMBean
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public interface SynchronizationMBeanImplMBean {

    /**
     * Get number of connection thread
     */
    public int getConnectionPoolThreads();

    /**
     * Get number of synch download tasks in the queue
     */
    public int getQueue();

    public long getTasks();

    public int getActiveThreads();

    public int getPoolSize();
}


