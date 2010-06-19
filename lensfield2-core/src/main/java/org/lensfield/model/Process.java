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

    private String name;
    private String classname;

    private List<Parameter> parameters = new ArrayList<Parameter>();
    private List<Dependency> dependencies = new ArrayList<Dependency>();

    public Process(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null argument: name");
        }
        this.name = name;
    }

    public Process(String name, String classname) {
        this.name = name;
        this.classname = classname;
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


    public String getClassName() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }
}
