/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class Operation {

    private Map<String, List<FileState>> inputFiles;
    private Map<String, List<FileState>> outputFiles;

    public Operation(Map<String, List<FileState>> inputFiles, Map<String, List<FileState>> outputFiles) {
        this.inputFiles = inputFiles;
        this.outputFiles = outputFiles;
    }

    public Map<String, List<FileState>> getInputFiles() {
        return inputFiles;
    }

    public Map<String, List<FileState>> getOutputFiles() {
        return outputFiles;
    }
    
}
