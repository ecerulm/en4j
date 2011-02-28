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
package com.rubenlaguna.en4j.mainmodule;

import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class RepeatableTask {

    private final Task task;
    private final int delay;
    private long lastRun = 0;

    public RepeatableTask(final Runnable runnable, int i) {
        delay = i;
        task = new RequestProcessor().create(new Runnable() {

            @Override
            public void run() {
                waitTurn();
                runnable.run();
            }
        }, true);
    }

    public void schedule() {
        task.schedule(timeToWait());
    }

    private void waitTurn() {
        long toWait = timeToWait();
        if (toWait > 0) {
            try {
                Thread.sleep(toWait);
            } catch (InterruptedException ex) {
            }
        }
        lastRun = System.currentTimeMillis();
    }

    public int timeToWait() {
        long toWait = lastRun + delay - System.currentTimeMillis();
        if (toWait < 0) {
            toWait = 0;
        } else {
            if (toWait > delay) {
                toWait = delay;
            }
        }

        return (int) toWait;
    }
}
