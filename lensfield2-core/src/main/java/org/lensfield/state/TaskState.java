/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import org.lensfield.DebugClassLoader;
import org.lensfield.build.InputDescription;
import org.lensfield.build.OutputDescription;
import org.lensfield.build.ParameterDescription;
import org.lensfield.glob.Template;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author sea36
 */
public class TaskState {

    private String id;
    private List<DependencyState> dependencyList = new ArrayList<DependencyState>();
    private long lastModified = System.currentTimeMillis();

    private transient Map<String,ParameterDescription> parameterDescriptions = new HashMap<String,ParameterDescription>();
    private transient Map<String, InputDescription> inputDescriptions = new HashMap<String, InputDescription>();
    private transient Map<String, OutputDescription> outputDescriptions = new HashMap<String, OutputDescription>();
    private transient boolean noArgs;
    private transient boolean updated = true;

    private String className;

    private String methodName = "run";
    private String methodClass;
    private Class<?>[] methodParams;

    private ClassLoader parentClassloader;
    private URL[] dependencyUrls;

    private List<Operation> operations = new ArrayList<Operation>();

    public TaskState(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        if (className.indexOf('/') != -1) {
            int i = className.indexOf('/');
            methodName = className.substring(1 + i);
            className = className.substring(0, i);
        }
        this.className = className;
    }


    public List<DependencyState> getDependencyList() {
        return dependencyList;
    }

    public void setDependencyList(List<DependencyState> dependencyList) {
        this.dependencyList = dependencyList;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void addDependency(DependencyState dependency) {
        dependencyList.add(dependency);
    }


    public void addParameter(ParameterDescription parameter) {
        parameterDescriptions.put(parameter.getName(), parameter);
    }

    public List<ParameterDescription> getParameters() {
        return new ArrayList<ParameterDescription>(parameterDescriptions.values());
    }


    public void addInput(InputDescription input) {
        inputDescriptions.put(input.getName(), input);
    }

    public List<InputDescription> getInputs() {
        return new ArrayList<InputDescription>(inputDescriptions.values());
    }


    public void addOutput(OutputDescription output) {
        outputDescriptions.put(output.getName(), output);
    }

    public List<OutputDescription> getOutputs() {
        return new ArrayList<OutputDescription>(outputDescriptions.values());
    }

    public OutputDescription getOutput(String name) {
        return outputDescriptions.get(name);
    }


    public void setMethod(Method method, boolean noArgs) {
        this.methodName = method.getName();
        this.methodClass = method.getDeclaringClass().getName();
        this.methodParams = method.getParameterTypes();
        this.noArgs = noArgs;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodClass() {
        return methodClass;
    }

    public Class<?>[] getMethodParams() {
        return methodParams;
    }

    public boolean isNoArgs() {
        return noArgs;
    }



    public InputDescription getDefaultInput() {
        if (inputDescriptions.size() == 1) {
            return inputDescriptions.values().iterator().next();
        }
        return null;
    }

    public OutputDescription getDefaultOutput() {
        if (outputDescriptions.size() == 1) {
            return outputDescriptions.values().iterator().next();
        }
        return null;
    }

    public ParameterDescription getDefaultParameter() {
        if (parameterDescriptions.size() == 1) {
            return parameterDescriptions.values().iterator().next();
        }
        return null;
    }

    public boolean isKtoL() {
        for (InputDescription input : inputDescriptions.values()) {
            if (input.isMultifile()) {
                return false;
            }
        }
        for (OutputDescription output : outputDescriptions.values()) {
            if (output.isMultifile()) {
                return false;
            }
        }
        return true;
    }

    public boolean isNtoK() {
        for (InputDescription input : inputDescriptions.values()) {
            if (!input.isMultifile()) {
                return false;
            }
        }
        for (OutputDescription output : outputDescriptions.values()) {
            if (output.isMultifile()) {
                return false;
            }
        }
        return true;
    }

    public boolean isKtoN() {
        for (InputDescription input : inputDescriptions.values()) {
            if (input.isMultifile()) {
                return false;
            }
        }
        for (OutputDescription output : outputDescriptions.values()) {
            if (!output.isMultifile()) {
                return false;
            }
        }
        return true;
    }


    public void addOperation(Operation op) {
        operations.add(op);
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public Map<String,Operation> getInputOperationMap() {
        Map<String,Operation> map = new HashMap<String, Operation>();
        for (Operation op : operations) {
            for (List<FileState> files : op.getInputFiles().values()) {
                for (FileState file : files) {
                    map.put(file.getPath(),op);
                }
            }
        }
        return map;
    }

    public void setUpdated(boolean b) {
        this.updated = b;
    }

    public boolean isUpdated() {
        return updated;
    }

    public ParameterDescription getParameter(String name) {
        return parameterDescriptions.get(name);
    }

    public boolean isSource() {
        return this.getOperations().isEmpty() || this.getOperations().get(0).getInputFiles().isEmpty();
    }


    public void setDependencies(URL[] urls, ClassLoader parentClassloader) {
        this.dependencyUrls = urls;
        this.parentClassloader = parentClassloader;
    }


    public ClassLoader createClassLoader() {
        ClassLoader cl = new DebugClassLoader(dependencyUrls, parentClassloader, "("+id+")");
        return cl;
    }


}
