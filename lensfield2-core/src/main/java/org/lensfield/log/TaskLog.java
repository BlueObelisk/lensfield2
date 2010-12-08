package org.lensfield.log;

import org.lensfield.state.Dependency;
import org.lensfield.state.Operation;
import org.lensfield.state.Parameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sam Adams
 */
public class TaskLog {

    private String name;
    private String className;
    private long lastModified;

    private Map<String, Parameter> parameters = new LinkedHashMap<String,Parameter>();

    private List<Dependency> dependencies = new ArrayList<Dependency>();

    private List<OperationLog> operations = new ArrayList<OperationLog>();

    public TaskLog(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void addDependency(Dependency dependency) {
        this.dependencies.add(dependency);
    }

    public void addParameter(Parameter parameter) {
        this.parameters.put(parameter.getName(), parameter);
    }

    public boolean isSource() {
        return operations.isEmpty() ||
                operations.get(0).hasInputs();
    }

    public List<OperationLog> getOperations() {
        return operations;
    }

    public Parameter getParameter(String name) {
        return parameters.get(name);
    }

    public List<Dependency> getDependencyList() {
        return dependencies;
    }
    
}
