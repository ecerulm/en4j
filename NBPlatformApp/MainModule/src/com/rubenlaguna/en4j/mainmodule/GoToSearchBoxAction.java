/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.mainmodule;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class GoToSearchBoxAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        // TODO implement action body
        NoteListTopComponent.findInstance().requestActive();
        NoteListTopComponent.findInstance().searchTextField.requestFocusInWindow();

    }
}
