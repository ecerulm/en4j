/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.NoteContentViewModule;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.swing.EmptyReplacedElement;
import org.xhtmlrenderer.swing.ImageReplacedElement;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;
import org.xhtmlrenderer.util.ImageUtil;

/**
 *
 * @author ecerulm
 */
class ENMLReplacedElementFactory implements ReplacedElementFactory {

    private final SwingReplacedElementFactory delegate;

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


        Logger.getLogger(ENMLReplacedElementFactory.class.getName()).log(Level.INFO, "Element:"+box.getElement().getNodeName());
        Logger.getLogger(ENMLReplacedElementFactory.class.getName()).log(Level.INFO, "Element content:"+box.getElement().getNodeValue());

        if ("en-media".equals(box.getElement().getNodeName())) {
            toReturn = brokenImage(cssWidth,cssHeight);
        }

        if (null == toReturn) {
            toReturn = delegate.createReplacedElement(context, box, uac, cssWidth, cssHeight);
        }

        return toReturn;
    }

    private ReplacedElement brokenImage(int cssWidth, int cssHeight) {
        BufferedImage missingImage = null;
        ReplacedElement mre;
        try {
            // TODO: we can come up with something better; not sure if we should use Alt text, how text should size, etc.
            missingImage = ImageUtil.createCompatibleBufferedImage(cssWidth, cssHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = missingImage.createGraphics();
            g.setColor(Color.BLACK);
            g.setBackground(Color.WHITE);
            g.setFont(new Font("Serif", Font.PLAIN, 12));
            g.drawString("Missing", 0, 12);
            g.dispose();
            mre = new ImageReplacedElement(missingImage, cssWidth, cssHeight);
        } catch (Exception e) {
            mre = new EmptyReplacedElement(
                    cssWidth < 0 ? 0 : cssWidth,
                    cssHeight < 0 ? 0 : cssHeight);
        }
        return mre;
    }
}
