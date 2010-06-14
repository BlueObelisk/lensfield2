/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class InputFileSet {

    private Map<String,String> params;
    private Map<String,List<InputFileState>> files;

    public InputFileSet(Map<String, String> params, Map<String, List<InputFileState>> files) {
        this.params = params;
        this.files = files;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public Map<String, List<InputFileState>> getFiles() {
        return files;
    }

}
