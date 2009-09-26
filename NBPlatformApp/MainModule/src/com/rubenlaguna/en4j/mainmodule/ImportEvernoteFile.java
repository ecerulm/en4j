/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.mainmodule;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import org.openide.filesystems.FileChooserBuilder;

public final class ImportEvernoteFile implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        // TODO implement action body
         //The default dir to use if no value is stored
      File home = new File (System.getProperty("user.home") + File.separator + "lib");
      //Now build a file chooser and invoke the dialog in one line of code
      //"libraries-dir" is our unique key
      File toAdd = new FileChooserBuilder ("import-dir").setTitle("Import Evernote File").setDefaultWorkingDirectory(home).setApproveText("Add").showOpenDialog();
     //Result will be null if the user clicked cancel or closed the dialog w/o OK
      if (toAdd != null) {
          //do something
      }


        NoteListTopComponent.findInstance();
    }
}
