/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.build.FileList;
import org.lensfield.glob.Template;
import org.lensfield.io.StreamOut;
import org.lensfield.io.StreamOutImpl;
import org.lensfield.state.FileState;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sea36
 */
public class OutputFileState extends FileState {

    private String outputName;
    private File tempFile;
    private Template glob;
    private FileList state;
    private StreamOut stream;

    public OutputFileState(String path, long lastModified, Map<String, String> params, File tempFile, Template glob, StreamOut stream, FileList state) {
        super(path, lastModified, params);
        this.tempFile = tempFile;
        this.glob = glob;
        this.stream = stream;
        this.state = state;
    }

    public OutputFileState(String name) {
        this.outputName = name;
    }

    public String getOutputName() {
        return outputName;
    }

    public File getTempFile() {
        return tempFile;
    }

    public void setTempFile(File tempFile) {
        this.tempFile = tempFile;
    }

    public synchronized StreamOut getStream() throws IOException {
        if (stream == null) {
            stream = new StreamOutImpl(tempFile, getParams());
        }
        return stream;
    }

    public Template getGlob() {
        return glob;
    }

    public void setGlob(Template glob) {
        this.glob = glob;
    }

    public FileList getState() {
        return state;
    }

    public void setState(FileList state) {
        this.state = state;
    }

}
