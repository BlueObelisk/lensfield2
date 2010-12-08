/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import org.lensfield.concurrent.OperationRunner;
import org.lensfield.concurrent.Reactor;
import org.lensfield.glob.Glob;
import org.lensfield.glob.GlobAnalyser;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * @author sea36
 */
public class Process {

    private static final Object TOKEN = new Object();

    private final String id;
    private final Reactor reactor;

    private volatile boolean active = false;

    private List<Dependency> dependencyList = new ArrayList<Dependency>();
    private long lastModified = System.currentTimeMillis();

    private transient Map<String, Parameter> parameters = new HashMap<String, Parameter>();
    private transient Map<String, InputPipe> inputs = new HashMap<String, InputPipe>();
    private transient Map<String, OutputPipe> outputs = new HashMap<String, OutputPipe>();
    
    private transient boolean noArgs;
    private transient boolean updated = true;

    private String className;

    private String methodName = "run";
    private String methodClass;
    private Class<?>[] methodParams;

    private ClassLoader parentClassloader;
    private URL[] dependencyUrls;

    private Queue<Operation> opQueue = new ConcurrentLinkedQueue<Operation>();

    private ConcurrentMap<OperationKey,Operation> opMap = new ConcurrentHashMap<OperationKey, Operation>();
    private ConcurrentMap<Operation,Object> opSet = new ConcurrentHashMap<Operation,Object>();

    private List<String> globNames;


    public Process(String id, Reactor reactor) {
        this.id = id;
        this.reactor = reactor;
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


    public List<Dependency> getDependencyList() {
        return dependencyList;
    }

    public void setDependencyList(List<Dependency> dependencyList) {
        this.dependencyList = dependencyList;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void addDependency(Dependency dependency) {
        dependencyList.add(dependency);
    }


    public void addParameter(Parameter parameter) {
        parameters.put(parameter.getName(), parameter);
    }

    public List<Parameter> getParameters() {
        return new ArrayList<Parameter>(parameters.values());
    }


    public void addInput(InputPipe input) {
        inputs.put(input.getName(), input);
    }

    public List<InputPipe> getInputs() {
        return new ArrayList<InputPipe>(inputs.values());
    }

    public InputPipe getInput(String name) {
        return inputs.get(name);
    }


    public void addOutput(OutputPipe output) {
        outputs.put(output.getName(), output);
    }

    public List<OutputPipe> getOutputs() {
        return new ArrayList<OutputPipe>(outputs.values());
    }

    public OutputPipe getOutput(String name) {
        return outputs.get(name);
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



    public InputPipe getDefaultInput() {
        if (inputs.size() == 1) {
            return inputs.values().iterator().next();
        }
        return null;
    }

    public OutputPipe getDefaultOutput() {
        if (outputs.size() == 1) {
            return outputs.values().iterator().next();
        }
        return null;
    }


    public boolean isKtoL() {
        for (InputPipe input : inputs.values()) {
            if (input.isMultifile()) {
                return false;
            }
        }
        for (OutputPipe output : outputs.values()) {
            if (output.isMultifile()) {
                return false;
            }
        }
        return true;
    }

    public boolean isKtoN() {
        for (InputPipe input : inputs.values()) {
            if (input.isMultifile()) {
                return false;
            }
        }
        for (OutputPipe output : outputs.values()) {
            if (!output.isMultifile()) {
                return false;
            }
        }
        return true;
    }


    public void queue(Operation op) {
        op.setQueued();
        opQueue.add(op);
        if (!active) {
            activate();
        }
    }

    private synchronized void activate() {
        if (!active) {
            reactor.queue(this);
            active = true;
        }
    }

//    public List<Operation> getOperations() {
//        return operations;
//    }

//    public Map<String,Operation> getInputOperationMap() {
//        Map<String,Operation> map = new HashMap<String, Operation>();
//        for (Operation op : operations) {
//            for (List<FileState> files : op.getInputResourcesMap().values()) {
//                for (FileState file : files) {
//                    map.put(file.getPath(),op);
//                }
//            }
//        }
//        return map;
//    }

    public void setUpdated(boolean b) {
        this.updated = b;
    }

    public boolean isUpdated() {
        return updated;
    }

    public Parameter getParameter(String name) {
        return parameters.get(name);
    }

    public boolean isSource() {
        return inputs.isEmpty();
    }


    public void setDependencyUrls(URL[] urls, ClassLoader parentClassloader) {
        this.dependencyUrls = urls;
        this.parentClassloader = parentClassloader;
    }


    public ClassLoader createClassLoader() {
        ClassLoader cl = new URLClassLoader(dependencyUrls, parentClassloader);
        return cl;
    }


    public Operation getOperation(OperationKey opKey) {
        Operation op = opMap.get(opKey);
        if (op == null) {
            op = new Operation(opKey);
            Operation prev = opMap.putIfAbsent(opKey, op);
            if (prev != null) {
                return prev;
            }
            opSet.put(op,TOKEN);
        }
        return op;
    }

    public boolean isFinished() {
        if (opSet.isEmpty()) {
            for (InputPipe input : inputs.values()) {
                if (!input.isClosed()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public Operation poll() {
        return opQueue.poll();
    }

    public boolean hasInputs() {
        return !inputs.isEmpty();
    }

    public void initGlobNames() {
        List<InputPipe> inputs = new ArrayList<InputPipe>(this.inputs.values());
        List<OutputPipe> outputs = new ArrayList<OutputPipe>(this.outputs.values());
        Glob[] globs = new Glob[inputs.size()+outputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            globs[i] = inputs.get(i).getSource().getGlob();
        }
        for (int i = 0; i < outputs.size(); i++) {
            globs[inputs.size()+i] = outputs.get(i).getGlob();
        }
        globNames = Collections.unmodifiableList(new ArrayList<String>(GlobAnalyser.getCommonGroups(globs)));
    }

    public List<String> getCommonGlobNames() {
        return globNames;
    }

    public Reactor getReactor() {
        return reactor;
    }

    public synchronized void close() {
        for (OutputPipe output : outputs.values()) {
            output.close();
        }
    }

    public OperationRunner getRunner() throws Exception {
        return reactor.getOperationRunner(this);
    }

    public void check() {
        for (Operation op : opMap.values()) {
            if (!op.isQueued() && op.isReady()) {
                queue(op);
            }
        }
    }

    public void handleOperationFinished(Operation operation) {
        opSet.remove(operation);
    }

}
