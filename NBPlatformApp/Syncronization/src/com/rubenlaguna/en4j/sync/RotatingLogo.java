/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.sync;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.interpolation.PropertySetter;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public class RotatingLogo extends JComponent {

    private static final Logger LOG = Logger.getLogger(RotatingLogo.class.getName());
    private BufferedImage image;
    private double rotateFactor = 0.0f;

    public double getRotateFactor() {
        return rotateFactor;
    }

    public void setRotateFactor(double rotateFactor) {
        this.rotateFactor = rotateFactor;
        LOG.info("rotateFactor = " + rotateFactor);
        repaint();
    }
    public RotatingLogo(String imageName) {
        try {
            image = GraphicsUtilities.loadCompatibleImage(
                    getClass().getResource(imageName));
            LOG.info("image loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        startAnimator();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(image.getWidth(), image.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        int x = (getWidth() - image.getWidth()) / 2;
        int y = (getHeight() - image.getHeight()) / 2;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.rotate(rotateFactor * 2.0 * Math.PI,
                image.getWidth() / 2.0,
                image.getHeight() / 2.0);
        g2d.drawImage(image, 0, 0, null);
    }

    private void startAnimator() {
        PropertySetter setter = new PropertySetter(this, "rotateFactor", 0.0f, 1.0f);
        Animator animator = new Animator(5000,Float.POSITIVE_INFINITY, Animator.RepeatBehavior.LOOP, setter);
        animator.start();
    }
}
