/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import org.lensfield.Logger;
import org.lensfield.state.FileState;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author sea36
 */
public class MultiStreamInImpl extends MultiStreamIn {

    private Logger log;
    private List<FileState> files;
    private File root;
    private volatile int i = 0;

    public MultiStreamInImpl(File root, List<FileState> fs, Logger log) {
        this.root = root;
        this.files = new ArrayList<FileState>(fs);
        this.log = log;
    }

    @Override
    public InputStream next() throws IOException {
        if (i >= files.size()) {
            return null;
        }
        FileState f = files.get(i++);
        log.debug("reading "+f.getPath());
        return new StreamInImpl(new File(root, f.getPath()), new HashMap<String, String>(f.getParams()));
    }
    
}
