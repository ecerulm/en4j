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
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
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
            LOG.log(Level.FINE, "en-media detected");

            final String type = box.getElement().getAttribute("type");
            String hash = box.getElement().getAttribute("hash");

            if (isImage(type)) {
//                LOG.log(Level.FINE, "en-media type: image/jpeg");
                LOG.log(Level.FINE, "en-media hash: " + hash);

                toReturn = loadImage(context, hash);
                if (null == toReturn) {
                    toReturn = brokenImage(context, 100, 100);
                }
                //
                //    toReturn = loadImage()
                //            return toReturn;
                //} else if("im") {

            } else {
                toReturn = unrecognizedResource(context, hash);
            }
        }

        if (null == toReturn) {
//            LOG.log(Level.INFO, "delegating to next factory:" + box.getElement().getNodeName());
            toReturn = delegate.createReplacedElement(context, box, uac, cssWidth, cssHeight);
        }

        return toReturn;
    }

    private boolean isImage(final String type) {
        boolean toReturn = false;
        toReturn = toReturn || "image/jpeg".equalsIgnoreCase(type);
        toReturn = toReturn || "image/gif".equalsIgnoreCase(type);
        toReturn = toReturn || "image/png".equalsIgnoreCase(type);
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

        InputStream is = getImage(hash);
        Image image = null;
        if (is == null) {
            return brokenImage(context, 100, 100);
        }
        try {
            image = ImageIO.read(is);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "exception caught:", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        if (image == null) {
            return brokenImage(context, 100, 100);
        }

        ImageIcon icon = new ImageIcon(image);

        JLabel cc = new JLabel(icon);
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

    private InputStream getImage(String hash) {
        assert (null != this.note);
        final Resource resource = this.note.getResource(hash);
        if (null == resource) {
            return null;
        }
        return resource.getDataAsInputStream();
    }
//    private byte[] getImage(String hash) {
//        assert (null != this.note);
//        final Resource resource = this.note.getResource(hash);
//        if (null == resource) {
//            return null;
//        }
//        return resource.getData();
//    }

    void setNote(Note n) {
        if (null == n) {
            throw new IllegalArgumentException("Note cannot be null");
        }
        this.note = n;
    }

    private ReplacedElement unrecognizedResource(LayoutContext context, String hash) {
        ReplacedElement toReturn = null;


        final Resource resource = this.note.getResource(hash);
        UnrecognizedResourceJPanel cc = new UnrecognizedResourceJPanel(resource);

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
    }
}
