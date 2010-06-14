/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.build.FileList;

/**
 * @author sea36
 */
public class OutputState {

    private String name;
    private FileList state;

    public OutputState(String name, FileList state) {
        this.name = name;
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public FileList getState() {
        return state;
    }
    
}