/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class Build extends Process {

    private final Map<String,Input> inputs = new LinkedHashMap<String,Input>();
    private final Map<String,Output> outputs = new LinkedHashMap<String,Output>();

    public Build(String name, String classname) {
        super(name, classname);
    }

    public Build(String name, String classname, String input, String output) {
        super(name, classname);
        this.inputs.put(null, new Input(input));
        this.outputs.put(null, new Output(output));
    }

    
    public void addInput(Input input) {
        if (inputs.containsKey(input.getName())) {
            throw new IllegalArgumentException("Already has input '"+input.getName()+"'");
        }
        inputs.put(input.getName(), input);
    }

    public List<Input> getInputs() {
        return new ArrayList<Input>(inputs.values());
    }


    public void addOutput(Output output) {
        if (outputs.containsKey(output.getName())) {
            throw new IllegalArgumentException("Already has output '"+output.getName()+"'");
        }
        outputs.put(output.getName(), output);
    }

    public List<Output> getOutputs() {
        return new ArrayList<Output>(outputs.values());
    }

}
