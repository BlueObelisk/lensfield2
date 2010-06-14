/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.build.FileList;
import org.lensfield.glob.Template;
import org.lensfield.io.MultiStreamOut;

import java.io.File;
import java.util.Map;

/**
 * @author sea36
 */
public class OutputMultiFileState {

    private String name;
    private MultiStreamOut out;
    private Template glob;
    private Map<String,String> params;
    private FileList state;

    public OutputMultiFileState(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public MultiStreamOut getOutput() {
        return out;
    }

    public void setOutput(MultiStreamOut out) {
        this.out = out;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
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