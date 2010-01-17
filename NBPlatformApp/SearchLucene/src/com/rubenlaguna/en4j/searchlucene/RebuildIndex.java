/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.interfaces.NoteFinder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
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

    public void actionPerformed(ActionEvent e) {


        //TODO use an Executor/ RequestProcessor here
        LOG.info("Rebuild index");

        final NoteFinder noteFinder = Lookup.getDefault().lookup(NoteFinder.class);

        RequestProcessor.Task theTask = RP.post(new Runnable() {

            public void run() {
                noteFinder.rebuildIndex();

            }
        });

        final ProgressHandle myProgressHandle =
                ProgressHandleFactory.createHandle("Rebuilding index", theTask);
        myProgressHandle.start();
        theTask.addTaskListener(new TaskListener() {

            public void taskFinished(Task task) {
                myProgressHandle.finish();
            }
        });


    }
}
