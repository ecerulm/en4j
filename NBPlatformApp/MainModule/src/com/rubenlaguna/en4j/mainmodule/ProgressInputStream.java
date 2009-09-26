/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.mainmodule;

import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Ruben Laguna <ruben.laguna at gmail.com>
 */
class ProgressInputStream extends FilterInputStream {

    private final ProgressListener pl;
    private int read = 0;
    private int last = 0;
    private  int stepsize = 0;

    public ProgressInputStream(InputStream fileInputStream,int stepsize, ProgressListener progressListener) {
        super(fileInputStream);
        this.stepsize = stepsize;
        this.pl = progressListener;
    }

    @Override
    public int read() throws IOException {
        int c = super.read();
        updateProgress(1);
        return c;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int x = super.read(b);
        updateProgress(x);
        return x;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int x = super.read(b, off, len);
        updateProgress(x);
        return x;
    }

    private void updateProgress(int x) {
        if (x > 0) {
            read = read + x;
        }
        if (read - last > stepsize) {
            pl.progress(read);
            System.out.println(""+read);
            last = read;
        }
    }
}
