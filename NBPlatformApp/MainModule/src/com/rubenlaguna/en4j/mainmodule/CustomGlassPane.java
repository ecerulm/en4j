/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.mainmodule;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.util.logging.Logger;
import javax.swing.JComponent;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class CustomGlassPane extends JComponent {

    private static final Logger LOG = Logger.getLogger(CustomGlassPane.class.getName());
    private float BAR_WIDTH = 100;
    private int BAR_HEIGHT = 10;

    public CustomGlassPane() {
        addMouseListener(new MouseAdapter() {
        });
        addMouseMotionListener(new MouseMotionAdapter() {
        });
        addKeyListener(new KeyAdapter() {
        });

    }

    @Override
    protected void paintComponent(Graphics g) {
        LOG.info("customglasspane paint");
        // gets the current clipping area
        Rectangle clip = g.getClipBounds();
// sets a 65% translucent composite
        Graphics2D g2 = (Graphics2D) g.create();
        AlphaComposite alpha = AlphaComposite.SrcOver.derive(0.65f);
        g2.setComposite(alpha);
// fills the background
        g2.setColor(getBackground());
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);
// computes x and y, draws the text // [...] snipped [...]
        int x = 100;
        int y = 100;
// computes the size of the progress indicator
        int w = (int) (BAR_WIDTH * ((float) 50.0f / 100.0f));
        int h = BAR_HEIGHT;
// draws the content of the progress bar
        g2.setColor(Color.BLACK);
        g2.drawString("test", 50, 50);
        g2.fillRect(x, y, w, h);
    }
}
