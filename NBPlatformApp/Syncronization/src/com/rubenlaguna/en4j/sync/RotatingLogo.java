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
package com.rubenlaguna.en4j.sync;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.interpolation.PropertySetter;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public class RotatingLogo extends JComponent implements Serializable {

    private static final Logger LOG = Logger.getLogger(RotatingLogo.class.getName());
    private BufferedImage image;
    private double rotateFactor = 0.0f;
    private PropertySetter setter = new PropertySetter(this, "rotateFactor", 0.0f, 1.0f);
    private Animator animator = new Animator(5000, Float.POSITIVE_INFINITY, Animator.RepeatBehavior.LOOP, setter);


    public RotatingLogo() {
        this("/com/rubenlaguna/en4j/sync/sync.png");
    }
    public RotatingLogo(String imageName) {
        try {
            image = GraphicsUtilities.loadCompatibleImage(
                    getClass().getResource(imageName));
            LOG.info("image loaded");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public double getRotateFactor() {
        return rotateFactor;
    }

    public void setRotateFactor(final double fraction) {
        rotateFactor = fraction;
//        LOG.info("this = "+this.hashCode()+" rotateFactor = " + getRotateFactor() );
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(image.getWidth(), image.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
//        LOG.info("this = "+this.hashCode()+" paint rotateFactor = " + getRotateFactor() );
        int x = (getWidth() - image.getWidth()) / 2;
        int y = (getHeight() - image.getHeight()) / 2;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.rotate(rotateFactor * 2.0 * Math.PI,
                image.getWidth() / 2.0,
                image.getHeight() / 2.0);
        g2d.drawImage(image, 0, 0, null);
    }

    public void startAnimator() {
        LOG.info("startAnimator");
        animator.start();
    }

    public void stopAnimator() {
        LOG.info("stopAnimator");
        animator.stop();
        setRotateFactor(0.0f);
    }
}
