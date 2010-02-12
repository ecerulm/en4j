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
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.TaskListener;

public final class RebuildIndex implements ActionListener {

    private static final RequestProcessor RP = new RequestProcessor("Rebuild index tasks", 1, true);
    private static final Logger LOG = Logger.getLogger(RebuildIndex.class.getName());

    public void actionPerformed(ActionEvent e) {


        //TODO use an Executor/ RequestProcessor here
        LOG.info("Rebuild index");

        final NoteFinder noteFinder = Lookup.getDefault().lookup(NoteFinder.class);

        RequestProcessor.Task theTask = RP.create(new Runnable() {

            public void run() {
                noteFinder.rebuildIndex();
            }
        });

        final ProgressHandle myProgressHandle =
                ProgressHandleFactory.createHandle("Rebuilding index", theTask);
        myProgressHandle.start();
        theTask.addTaskListener(new TaskListener() {

            public void taskFinished(Task taßsk) {
                myProgressHandle.finish();
            }
        });
        theTask.schedule(100);


    }
}
