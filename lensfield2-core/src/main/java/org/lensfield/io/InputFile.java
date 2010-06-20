/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.lensfield.api.io.StreamIn;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author sea36
 */
public class InputFile extends StreamIn implements Input {

    private String path;
    private File file;
    private Map<String,String> parameters;
    private BufferedInputStream in;


    public InputFile(String path, File file, Map<String,String> params) throws IOException {
        this.path = path;
        this.file = file;
        this.parameters = params;
        this.in = new BufferedInputStream(new AutoCloseInputStream(new FileInputStream(file)));
    }

    public void reopen() throws IOException {
        in.close();
        in = new BufferedInputStream(new FileInputStream(file));
    }

    @Override
    public String getParameter(String name) {
        return parameters.get(name);
    }

    public String getPath() {
        return path;
    }

    @Override
    public long length() {
        return file.length();
    }

    // --- Delegate Methods ---

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
