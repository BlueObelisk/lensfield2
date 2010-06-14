/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.io.StreamIn;
import org.lensfield.io.StreamInImpl;
import org.lensfield.state.FileState;

import java.io.File;
import java.io.IOException;

/**
 * @author sea36
 */
public class InputFileState extends FileState {

    private File file;

    public InputFileState(File file, String path) {
        super(path, file.lastModified());
        this.file = file;
    }

    public StreamIn getStream() throws IOException {
        StreamIn stream = new StreamInImpl(file, getParams());
        return stream;
    }

    public File getFile() {
        return file;
    }
    
}
