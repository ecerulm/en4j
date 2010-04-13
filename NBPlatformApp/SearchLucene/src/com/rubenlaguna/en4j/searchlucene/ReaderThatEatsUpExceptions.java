/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class ReaderThatEatsUpExceptions extends Reader {

    private static final Logger LOG = Logger.getLogger(ReaderThatEatsUpExceptions.class.getName());
    private Reader delegate;
    private InputStream is;

    ReaderThatEatsUpExceptions(Reader docTikaReader, InputStream dataAsInputStream) {
        this.delegate = docTikaReader;
        this.is = dataAsInputStream;
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean ready() throws IOException {
        return delegate.ready();
    }

    public int read(char[] cbuf, int off, int len) {
        try {
            return delegate.read(cbuf, off, len);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "exception caught:", ex);
            return -1;
        }
    }

    @Override
    public int read(char[] cbuf) {
        try {
            return delegate.read(cbuf);
        } catch (IOException ex) {
            //LOG.log(Level.WARNING, "exception caught:", ex);
            LOG.log(Level.WARNING, "exception caught:"+ ex.getMessage());
            return -1;
        }
    }

    @Override
    public int read() {
        try {
            return delegate.read();
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "exception caught:", ex);
            return -1;
        }
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        try {
            return delegate.read(target);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "exception caught:", ex);
            return -1;
        }
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    public void mark(int readAheadLimit) throws IOException {
        delegate.mark(readAheadLimit);
    }

    public void close() throws IOException {
        try {
            is.close();
        } catch (IOException e) {
        }
        is = null;
        delegate.close();
        delegate = null;
    }
}
