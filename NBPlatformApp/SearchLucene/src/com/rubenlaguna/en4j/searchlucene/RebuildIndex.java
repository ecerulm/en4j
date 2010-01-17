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

public final class RebuildIndex implements ActionListener {

    private static final RequestProcessor RP = new RequestProcessor("Rebuild index tasks", 1, true);
    private static final Logger LOG = Logger.getLogger(RebuildIndex.class.getName());

    public void actionPerformed(ActionEvent e) {


        //TODO use an Executor/ RequestProcessor here
        LOG.info("Rebuild index");

        final NoteFinder noteFinder = Lookup.getDefault().lookup(NoteFinder.class);

        final List<Cancellable> link = new ArrayList<Cancellable>();

        final ProgressHandle myProgressHandle =
                ProgressHandleFactory.createHandle("Rebuilding index", new Cancellable() {

            public boolean cancel() {
                if (link.isEmpty()) {
                    return false;
                }
                final boolean cancel = link.get(0).cancel();
                return cancel;
            }
        });

        RequestProcessor.Task theTask = RP.post(new Runnable() {

            public void run() {
                myProgressHandle.start();
                noteFinder.rebuildIndex();
                myProgressHandle.finish();

            }
        });
        link.add(theTask);
    }
}
