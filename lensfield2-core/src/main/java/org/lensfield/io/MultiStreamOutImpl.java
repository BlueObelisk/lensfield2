/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import org.lensfield.OutputFileState;
import org.lensfield.glob.Template;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author sea36
 */
public class MultiStreamOutImpl extends MultiStreamOut {

    private String name;
    private File tmpDir;
    private Template glob;
    private Map<String,String> parameters;

    private List<OutputFileState> outputs = new ArrayList<OutputFileState>();
    private int ix = 1;

    public MultiStreamOutImpl(String name, File tmpdir, Template glob, Map<String,String> parameters) {
        this.name = name;
        this.tmpDir = tmpdir;
        this.glob = glob;
        this.parameters = parameters;
    }

    @Override
    public synchronized StreamOut next() throws IOException {
        Map<String,String> params = new HashMap<String, String>(parameters);
        params.put("%i", Integer.toString(ix++));
        OutputFileState output = new OutputFileState(name);
        output.setTempFile(new File(tmpDir, UUID.randomUUID().toString()));
        output.setParams(params);
        outputs.add(output);
        return output.getStream();
    }

    public List<OutputFileState> getOutputs() {
        return outputs;
    }
    
}
