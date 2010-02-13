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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.TaskListener;

public final class RebuildIndex implements ActionListener {

    private static final RequestProcessor RP = new RequestProcessor("Rebuild index tasks", 1, true);
    private static final Logger LOG = Logger.getLogger(RebuildIndex.class.getName());
    private RequestProcessor.Task theTask = null;

    public void actionPerformed(ActionEvent e) {
        LOG.info("Rebuild index");

        final ProgressHandle ph = ProgressHandleFactory.createHandle("Rebuilding index", new Cancellable() {

            public boolean cancel() {
                return handleCancel();
            }
        });

        final NoteFinder noteFinder = Lookup.getDefault().lookup(NoteFinder.class);

        theTask = RP.create(new Runnable() {

            public void run() {
                noteFinder.rebuildIndex(ph);
            }
        });

        theTask.addTaskListener(new TaskListener() {

            public void taskFinished(Task task) {
                ph.finish();
            }
        });
        ph.start();
        theTask.schedule(500);


    }

    private boolean handleCancel() {
        if (null == theTask) {
            return false;
        }
        return theTask.cancel();
    }
}
