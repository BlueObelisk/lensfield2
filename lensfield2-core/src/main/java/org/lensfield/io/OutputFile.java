/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import org.lensfield.api.io.StreamOut;
import org.lensfield.glob.Glob;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author sea36
 */
public class OutputFile extends StreamOut implements Output {

    private File file;
    private Map<String,String> parameters;
    private Glob glob;
    private String path;


    private BufferedOutputStream out;

    public OutputFile(File file, Map<String, String> params) throws IOException {
        this.file = file;
        this.parameters = params;
        this.out = new BufferedOutputStream(new FileOutputStream(file));
    }

    public OutputFile(File file, Map<String, String> params, Glob glob) throws IOException {
        this.file = file;
        this.parameters = params;
        this.glob = glob;
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
        String pname;
        if ("*".equals(name) || "**".equals(name)) {
            pname = name;
        }
        else if ("**/".equals(name)) {
            pname = "**";
        }
        else {
            pname = "$"+name;
        }
        if (parameters.containsKey(name)) {
            throw new IllegalStateException("Parameter: "+name+" already set");
        }
        parameters.put(pname, value);
    }


    public File getFile() {
        return file;
    }

    public Glob getGlob() {
        return glob;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getParams() {
        return parameters;
    }

    public void setFile(File file) {
        this.file = file;
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
