/*
 * SearchLuceneMBeanImplMBean.java
 *
 * Created on March 2, 2010, 7:48 PM
 */

package com.rubenlaguna.en4j.searchlucene;

/**
 * Interface SearchLuceneMBeanImplMBean
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public interface SearchLuceneMBeanImplMBean
{

    /**
     * Get number of documents waiting to be indexed
     */
    public int getIndexQueueSize();
    
}


