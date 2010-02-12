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
package com.rubenlaguna.en4j.NoteContentViewModule;

import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.swing.SwingReplacedElement;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;

/**
 *
 * @author ecerulm
 */
class ENMLReplacedElementFactory implements ReplacedElementFactory {

    private final SwingReplacedElementFactory delegate;
    private final Logger LOG = Logger.getLogger(ENMLReplacedElementFactory.class.getName());
    private Note note = null;

    public ENMLReplacedElementFactory(SwingReplacedElementFactory delegate) {
        this.delegate = delegate;
    }

    public void setFormSubmissionListener(FormSubmissionListener fsl) {
        delegate.setFormSubmissionListener(fsl);
    }

    public void reset() {
        delegate.reset();
    }

    public void remove(Element e) {
        delegate.remove(e);
    }

    public ReplacedElement createReplacedElement(LayoutContext context, BlockBox box, UserAgentCallback uac, int cssWidth, int cssHeight) {
        ReplacedElement toReturn = null;

        //LOG.log(Level.INFO, "Element:" + box.getElement().getNodeName());
        //LOG.log(Level.INFO, "Element content:" + box.getElement().getNodeValue());

        if ("en-media".equals(box.getElement().getNodeName())) {
            LOG.log(Level.INFO, "en-media detected");

            if ("image/jpeg".equalsIgnoreCase(box.getElement().getAttribute("type"))) {
                LOG.log(Level.INFO, "en-media type: image/jpeg");
                String hash = box.getElement().getAttribute("hash");
                LOG.log(Level.INFO, "en-media hash: " + hash);

                toReturn = loadImage(context, hash);
                //
                //    toReturn = loadImage()
                //            return toReturn;
            } else {

                toReturn = brokenImage(context, 100, 100);
            }
        }

        if (null == toReturn) {
            toReturn = delegate.createReplacedElement(context, box, uac, cssWidth, cssHeight);
        }

        return toReturn;
    }

    private ReplacedElement brokenImage(LayoutContext context, int cssWidth, int cssHeight) {

        //TODO: add a real implementation that returns an image
        ReplacedElement toReturn = null;

        JTextArea cc = new JTextArea();
        cc.setText("Missing implementation for en-media");
        //cc.setPreferredSize(new Dimension(cssWidth, cssHeight));
        cc.setSize(cc.getPreferredSize());

        context.getCanvas().add(cc);

        toReturn = new SwingReplacedElement(cc) {

            public boolean isRequiresInteractivePaint() {
                return false;
            }
        };

        return toReturn;
    }

    private ReplacedElement loadImage(LayoutContext context, String hash) {

        //TODO: add a real implementation that returns an image
        ReplacedElement toReturn = null;

        //JTextArea cc = new JTextArea();
        //NoteRepository nr = Lookup.getDefault().lookup(NoteRepository.class);
        byte[] imageData = getImage(hash);
        ImageIcon icon = new ImageIcon(imageData);
        JLabel cc = new JLabel(icon);
        //cc.setText("Missing implementation for en-media");
        //cc.setPreferredSize(new Dimension(cssWidth, cssHeight));
        cc.setSize(cc.getPreferredSize());

        context.getCanvas().add(cc);

        toReturn = new SwingReplacedElement(cc) {

            public boolean isRequiresInteractivePaint() {
                return false;
            }
        };

        return toReturn;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    private byte[] getImage(String hash) {
        assert(null!=this.note);
        final Resource resource = this.note.getResource(hash);
        return resource.getData();
    }

    void setNote(Note n) {
        if (null == n){
            throw new IllegalArgumentException("Note cannot be null");
        }
        this.note=n;
    }
}
