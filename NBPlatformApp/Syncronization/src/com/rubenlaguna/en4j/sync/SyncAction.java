/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.sync;

import com.rubenlaguna.en4j.interfaces.SynchronizationService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

public final class SyncAction implements ActionListener {

    private final RequestProcessor RP = new RequestProcessor("sync task", 1, true);

    public void actionPerformed(ActionEvent e) {
        // TODO implement action body
        final SynchronizationService sservice = Lookup.getDefault().lookup(SynchronizationService.class);

        Runnable task = new Runnable() {

            public void run() {
                sservice.sync();
            }
        };
        RP.post(task);
    }
}
