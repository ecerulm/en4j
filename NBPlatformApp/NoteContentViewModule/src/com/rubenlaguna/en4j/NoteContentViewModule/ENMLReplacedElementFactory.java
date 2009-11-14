/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.NoteContentViewModule;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.swing.EmptyReplacedElement;
import org.xhtmlrenderer.swing.ImageReplacedElement;
import org.xhtmlrenderer.swing.SwingReplacedElement;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;
import org.xhtmlrenderer.util.ImageUtil;

/**
 *
 * @author ecerulm
 */
class ENMLReplacedElementFactory implements ReplacedElementFactory {

    private final SwingReplacedElementFactory delegate;
    private final Logger LOG = Logger.getLogger(ENMLReplacedElementFactory.class.getName());

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


        Logger.getLogger(ENMLReplacedElementFactory.class.getName()).log(Level.INFO, "Element:" + box.getElement().getNodeName());
        Logger.getLogger(ENMLReplacedElementFactory.class.getName()).log(Level.INFO, "Element content:" + box.getElement().getNodeValue());

        if ("en-media".equals(box.getElement().getNodeName())) {
            toReturn = brokenImage(context, 100, 100);
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
}
