/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import java.io.*;
import java.util.Map;

/**
 * @author sea36
 */
public class StreamOutImpl extends StreamOut {

    private File file;
    private Map<String,String> parameters;
    private BufferedOutputStream out;

    public StreamOutImpl(File file, Map<String, String> params) throws IOException {
        this.file = file;
        this.parameters = params;
        this.out = new BufferedOutputStream(new FileOutputStream(file));
    }

    public void reopen() throws IOException {
        checkOpen();
        out.close();
        out = new BufferedOutputStream(new FileOutputStream(file));
    }

    private void checkOpen() throws IOException {
        if (out == null) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public void setParameter(String name, String value) {
        parameters.put("$"+name, value);
    }


    // --- Delegate Methods ---


    @Override
    public void write(int b) throws IOException {
        checkOpen();
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkOpen();
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkOpen();
        out.flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkOpen();
        out.write(b);
    }

    @Override
    public void close() throws IOException {
        if (out != null) {
            out.close();
            out = null;
        }
    }
    
}
