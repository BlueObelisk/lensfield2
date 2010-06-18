/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sea36
 */
public abstract class Process {

    private final String name;

    private final List<Parameter> parameters = new ArrayList<Parameter>();
    private List<Dependency> dependencies = new ArrayList<Dependency>();

    public Process(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null argument: name");
        }
        this.name = name;
    }

    public Process(String name, String param) {
        this.name = name;
        this.parameters.add(new Parameter(param));
    }

    public String getName() {
        return name;
    }


    public List<Parameter> getParameters() {
        return parameters;
    }

    public void addParameter(Parameter parameter) {
        parameters.add(parameter);
    }


    public void addDependency(String groupId, String artifactId, String version) {
        dependencies.add(new Dependency(groupId, artifactId, version));
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

}
