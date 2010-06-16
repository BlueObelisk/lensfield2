/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import org.lensfield.api.io.MultiStreamOut;
import org.lensfield.api.io.StreamOut;
import org.lensfield.glob.Template;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author sea36
 */
public class OutputMultiFile extends MultiStreamOut implements Output {

    private File tmpDir;
    private Template glob;
    private Map<String,String> parameters;

    private List<OutputFile> outputs = new ArrayList<OutputFile>();
    private int ix = 1;

    public OutputMultiFile(File tmpdir, Template glob, Map<String,String> parameters) {
        this.tmpDir = tmpdir;
        this.glob = glob;
        this.parameters = parameters;
    }

    @Override
    public synchronized StreamOut next() throws IOException {
        File tmpFile = new File(tmpDir, UUID.randomUUID().toString());
        Map<String,String> params = new HashMap<String, String>(parameters);
        params.put("%i", Integer.toString(ix++));
        OutputFile output = new OutputFile(tmpFile, params, glob);
        outputs.add(output);
        return output;
    }

    public List<OutputFile> getOutputs() {
        return outputs;
    }

    public void close() {
        for (OutputFile out : outputs) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
}
