/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import org.lensfield.concurrent.ParameterSet;
import org.lensfield.concurrent.Resource;
import org.lensfield.concurrent.ResourceSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author sea36
 */
public class Operation {

    private final Map<InputPipe,ResourceSet> inputResourcesMap;
    private final Set<OutputPipe> outputSet;

    private final Process process;
    private final ParameterSet parameterSet;
    private volatile boolean queued;

    public Operation(OperationKey opKey) {
        this.process = opKey.getProcess();
        this.parameterSet = opKey.getParameterSet();
        this.inputResourcesMap = new HashMap<InputPipe,ResourceSet>();
        this.outputSet = new HashSet<OutputPipe>();
        for (InputPipe input : process.getInputs()) {
            inputResourcesMap.put(input,new ResourceSet(input));
        }
        for (OutputPipe output : process.getOutputs()) {
            outputSet.add(output);
        }
    }

    public Map<InputPipe,ResourceSet> getInputResourcesMap() {
        return inputResourcesMap;
    }

    public Set<OutputPipe> getOutputSet() {
        return outputSet;
    }

    public void addInputResource(InputPipe input, Resource resource) {
        inputResourcesMap.get(input).addResource(resource);
    }

    public boolean isReady() {
        for (ResourceSet rs : inputResourcesMap.values()) {
            if (!rs.isReadyAsInput()) {
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
        process.handleOperationFinished(this);
    }

}
