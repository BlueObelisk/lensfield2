/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import org.lensfield.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sea36
 */
public class InputMultiFile extends MultiStreamIn implements Input {

    private Logger log;
    private List<InputFile> inputs;
    private volatile int i = 0;

    public InputMultiFile(List<InputFile> inputs, Logger log) {
        this.inputs = new ArrayList<InputFile>(inputs);
        this.log = log;
    }

    @Override
    public StreamIn next() throws IOException {
        if (i >= inputs.size()) {
            return null;
        }
        InputFile f = inputs.get(i++);
        log.debug("reading "+f.getPath());
        return f;
    }

    public void close() {
        for (InputFile in : inputs) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
