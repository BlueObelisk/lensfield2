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
public class Model {

    private List<String> repositories = new ArrayList<String>();
    private List<Dependency> dependencies = new ArrayList<Dependency>();

    private Map<String,Process> processes = new LinkedHashMap<String, Process>();
    private List<Source> sources = new ArrayList<Source>();
    private List<BuildStep> builds = new ArrayList<BuildStep>();


    public void addRepository(String repository) {
        repositories.add(repository);
    }

    public List<String> getRepositories() {
        return repositories;
    }


    public void addDependency(String groupId, String artifactId, String version) {
        dependencies.add(new Dependency(groupId, artifactId, version));
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }


    public void addSource(Source source) {
        addProcess(source);
        sources.add(source);
    }

    public List<Source> getSources() {
        return sources;
    }


    public void addBuildStep(BuildStep build) {
        addProcess(build);
        builds.add(build);
    }

    public List<BuildStep> getBuildSteps() {
        return builds;
    }

    private void addProcess(Process proc) {
        if (processes.containsKey(proc.getName())) {
            throw new IllegalArgumentException("Process '"+proc.getName()+"' already defined");
        }
        processes.put(proc.getName(), proc);
    }

    public List<Process> getProcesses() {
        return new ArrayList<Process>(processes.values());
    }

    public Process getProcess(String name) {
        return processes.get(name);
    }

    public void addDependency(String id) {
        int i0 = id.indexOf(':');
        if (i0 == -1) {
            throw new IllegalArgumentException("Bad dependency id: "+id);
        }
        int i1 = id.indexOf(':', i0+1);
        if (i1 == -1) {
            throw new IllegalArgumentException("Bad dependency id: "+id);
        }
        String groupId = id.substring(0, i0);
        String artifactId = id.substring(i0+1, i1);
        String version = id.substring(i1+1);
        addDependency(groupId, artifactId, version);
    }
}
