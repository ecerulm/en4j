/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.TaskListener;

public final class ImportEvernoteFile implements ActionListener {

    private final static RequestProcessor RP = new RequestProcessor("import", 1, true);
    private final static Logger LOG = Logger.getLogger(ImportEvernoteFile.class.getName());

    public void actionPerformed(ActionEvent e) {
        // TODO implement action body
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

            Runnable task = new Runnable() {

                public void run() {
                    //TODO: Move all this to Database

                    NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
                    //

                    final ProgressHandle ph = ProgressHandleFactory.createHandle("import");
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
                        Lookup.getDefault().lookup(NoteFinder.class).rebuildIndex();



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
            RequestProcessor.Task importTask = RP.create(task);

            final ProgressHandle ph = ProgressHandleFactory.createHandle("importing file", importTask);
            ph.start();
            importTask.addTaskListener(new TaskListener() {

                public void taskFinished(Task task) {
                    ph.finish();
                }
            });

            importTask.schedule(500);

        }

    }
}

