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

package com.rubenlaguna.en4j.sync;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Class SynchronizationMBeanImpl
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public class SynchronizationMBeanImpl implements SynchronizationMBeanImplMBean {
    private ThreadPoolExecutor threadPoolExecutor=null;
    private int auth = 0;
    
    public SynchronizationMBeanImpl(){
    }

    /**
     * Get number of connection thread
     */
    public int getConnectionPoolThreads() {
        if (null==threadPoolExecutor){
            return 0;
        }
        return threadPoolExecutor.getPoolSize();
    }

    void setThreadPoolExecutor(ThreadPoolExecutor RP) {
        this.threadPoolExecutor = RP;
    }

    /**
     * Get number of synch download tasks in the queue
     */
    public int getQueue() {
        return threadPoolExecutor.getQueue().size();
    }

    public long getTasks() {
        return threadPoolExecutor.getTaskCount();
    }

    public int getActiveThreads() {
        return threadPoolExecutor.getActiveCount();
    }
    public int getPoolSize() {
        return threadPoolExecutor.getPoolSize();
    }

    public synchronized void  incrementReauthCounter() {
        auth++;
    }

    public int getNumberOfAuths() {
        return auth;
    }

}


