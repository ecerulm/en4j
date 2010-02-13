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

import com.rubenlaguna.en4j.interfaces.NoteFinder;
import com.rubenlaguna.en4j.interfaces.NoteRepository;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.TaskListener;

public final class ImportEvernoteFile implements ActionListener {

    private final static RequestProcessor RP = new RequestProcessor("import", 1, true);
    private final static Logger LOG = Logger.getLogger(ImportEvernoteFile.class.getName());
    private RequestProcessor.Task importTask = null;

    public void actionPerformed(ActionEvent e) {
        //The default dir to use if no value is stored
        File home = new File(System.getProperty("user.home") + File.separator + "lib");
        //Now build a file chooser and invoke the dialog in one line of code
        //"libraries-dir" is our unique key
        final File toAdd = new FileChooserBuilder("import-dir").setTitle("Import Evernote File").setDefaultWorkingDirectory(home).setApproveText("Add").showOpenDialog();
        //Result will be null if the user clicked cancel or closed the dialog w/o OK
        if (toAdd != null) {
            //do something
        }

        if (toAdd != null) {

            final ProgressHandle ph = ProgressHandleFactory.createHandle("importing file", new Cancellable() {

                public boolean cancel() {
                    return handleCancel();
                }
            });

            Runnable task = new Runnable() {

                public void run() {
                    //TODO: Move all this to Database

                    NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
                    //

                    try {

                        ph.start();

                        int available = (int) toAdd.length();
                        ph.switchToDeterminate(available);
                        InputStream in = null;
                        in = new BufferedInputStream(new ProgressInputStream(new FileInputStream(toAdd), available / 100, new ProgressListener() {

                            public void progress(int i) {
                                ph.progress(i);
                            }
                        }));

                        rep.importEntries(in, ph);
                        ph.progress("Rebuilding indexes");
                        Lookup.getDefault().lookup(NoteFinder.class).rebuildIndex(ph);



                        //NoteListTopComponent.findInstance().invalidate();
                    } catch (FileNotFoundException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (InterruptedException ex) {
                        LOG.info("import file was cancelled");
                    } finally {
                        ph.finish();
                        new SwingWorker() {

                            @Override
                            protected Object doInBackground() throws Exception {
                                return null;

                            }

                            @Override
                            protected void done() {
                                NoteListTopComponent.findInstance().refresh();

                            }
                        }.execute();
                    }

                }
            };
            importTask = RP.create(task);


            importTask.addTaskListener(new TaskListener() {

                public void taskFinished(Task task) {
                    ph.finish();
                }
            });

            importTask.schedule(500);

        }

    }

    private boolean handleCancel() {
        if (null == importTask) {
            return false;
        }
        return importTask.cancel();
    }
}

