/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import org.lensfield.concurrent.ParameterSet;
import org.lensfield.concurrent.Resource;
import org.lensfield.concurrent.ResourceSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author sea36
 */
public class Operation {

    private Map<Input,ResourceSet> inputFiles;
    private Set<Output> outputFiles;

    private Process process;
    private ParameterSet parameterSet;
    private volatile boolean queued;

    public Operation(OperationKey opKey) {
        this.process = opKey.getProcess();
        this.parameterSet = opKey.getParameterSet();
        this.inputFiles = new HashMap<Input,ResourceSet>();
        this.outputFiles = new HashSet<Output>();
        for (Input input : process.getInputs()) {
            inputFiles.put(input,new ResourceSet(input));
        }
        for (Output output : process.getOutputs()) {
            outputFiles.add(output);
        }
    }

    public Map<Input,ResourceSet> getInputFiles() {
        return inputFiles;
    }

    public Set<Output> getOutputFiles() {
        return outputFiles;
    }

    public void addResource(Input input, Resource resource) {
        inputFiles.get(input).add(resource);
    }

    public boolean isReady() {
        for (ResourceSet rs : inputFiles.values()) {
            if (!rs.isReady()) {
                return false;
            }
        }
        return true;
    }

    public Process getProcess() {
        return process;
    }

    public Map<String, String> getParameters() {
        return parameterSet.toMap();
    }

    public boolean isQueued() {
        return queued;
    }

    public void setQueued() {
        this.queued = true;
    }

    public void finished() {
        process.done(this);
    }

}
