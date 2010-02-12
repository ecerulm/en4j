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
    private int stepsize = 0;

    public ProgressInputStream(InputStream fileInputStream, int stepsize, ProgressListener progressListener) {
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
            System.out.println("" + read);
            last = read;
        }
    }
}
