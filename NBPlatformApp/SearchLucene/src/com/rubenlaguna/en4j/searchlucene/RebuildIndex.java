/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.interfaces.NoteFinder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                ProgressHandleFactory.createHandle("Cooking noodles, please wait...", new Cancellable() {
            public boolean cancel() {
                return link.get(0).cancel();
            }
        });

        RequestProcessor.Task theTask = RP.create(new Runnable() {

            public void run() {
                myProgressHandle.start();
                noteFinder.rebuildIndex();
                myProgressHandle.finish();

            }
        });
        link.add(theTask);

        RP.post(theTask);





    }
}
