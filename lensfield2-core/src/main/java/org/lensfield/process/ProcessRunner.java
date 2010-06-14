/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.process;

import org.lensfield.*;
import org.lensfield.build.InputDescription;
import org.lensfield.build.OutputDescription;
import org.lensfield.build.ParameterDescription;
import org.lensfield.io.*;
import org.lensfield.state.FileState;
import org.lensfield.state.TaskState;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class ProcessRunner {

    private TaskState task;
    private Class<?> clazz;
    private Method runMethod;
    private List<InputDescription> inputs;
    private List<OutputDescription> outputs;
    private List<ParameterDescription> parameters;

    private Logger LOG;

    public ProcessRunner(TaskState task) throws LensfieldException {
        this.task = task;
        inputs = new ArrayList<InputDescription>(task.getInputs());
        outputs = new ArrayList<OutputDescription>(task.getOutputs());
        parameters = new ArrayList<ParameterDescription>(task.getParameters());
        clazz = task.getClazz();

        if (!LensfieldProcess.class.isAssignableFrom(clazz)) {
            runMethod = task.getMethod();
        }

        checkClassInstantiable();
        ensureFieldsAccessible();
    }

    private void checkClassInstantiable() throws LensfieldException {
        try {
            clazz.newInstance();
        } catch (Exception  e) {
            throw new LensfieldException("Unable to create processor", e);
        }
    }

    private void ensureFieldsAccessible() {
        // TODO handle security exception
        if (task.isNoArgs()) {
            for (InputDescription input : inputs) {
                input.field.setAccessible(true);
            }
            for (OutputDescription output : outputs) {
                output.field.setAccessible(true);
            }
        }
        for (ParameterDescription param : parameters) {
            param.field.setAccessible(true);
        }
    }




    public LensfieldProcess configureKtoL(Map<String,StreamIn> inputs, Map<String,List<OutputFileState>> outputs) throws Exception {

        Object obj = clazz.newInstance();

        configureParameters(obj);

        if (!task.isNoArgs()) {
            StreamIn in = inputs.values().iterator().next();
            StreamOut out = outputs.values().iterator().next().get(0).getStream();
            return new ArgProxy(obj, runMethod, in, out);
        }
        configureStreamInputs(obj, inputs);
        configureStreamOutputs(obj, outputs);

        return proxy(obj);
    }

    public LensfieldProcess configureKtoN(Map<String,StreamIn> inputs, Map<String, OutputMultiFileState> outputs) throws Exception {

        Object obj = clazz.newInstance();

        configureStreamInputs(obj, inputs);
        configureMultiStreamOutputs(obj, outputs);
        configureParameters(obj);

        return proxy(obj);
    }

    public LensfieldProcess configureNtoK(Map<String,List<FileState>> inputs, Map<String, List<OutputFileState>> outputs, File root) throws Exception {

        Object obj = clazz.newInstance();

        configureMultiStreamInputs(obj, inputs, root);
        configureStreamOutputs(obj, outputs);
        configureParameters(obj);

        return proxy(obj);
    }


    private void configureParameters(Object obj) throws IllegalAccessException, LensfieldException {
        for (ParameterDescription param : this.parameters) {
            param.field.set(obj, param.value);
        }
    }

    private void configureStreamInputs(Object obj, Map<String,StreamIn> inputs) throws IllegalAccessException, LensfieldException, IOException {
        for (InputDescription input : this.inputs) {
            if (inputs.containsKey(input.name)) {
                StreamIn in = inputs.get(input.name);
                input.field.set(obj, in);
            } else {
                throw new LensfieldException("Missing input file: "+input.name);
            }
        }
    }

    private void configureMultiStreamInputs(Object obj, Map<String, List<FileState>> inputs, File root) throws IllegalAccessException, LensfieldException, IOException {
        for (InputDescription input : this.inputs) {
            if (inputs.containsKey(input.name)) {
                List<FileState> fs = inputs.get(input.name);
                MultiStreamIn in = new MultiStreamInImpl(root, fs, LOG);
                input.field.set(obj, in);
            } else {
                throw new LensfieldException("Missing input file: "+input.name);
            }
        }
    }

    private void configureStreamOutputs(Object obj, Map<String, List<OutputFileState>> outputs) throws IllegalAccessException, LensfieldException, IOException {
        for (OutputDescription output : this.outputs) {
            if (outputs.containsKey(output.name)) {
                StreamOut out = outputs.get(output.name).get(0).getStream();
                output.field.set(obj, out);
            } else {
                throw new LensfieldException("Missing output file: "+output.name);
            }
        }
    }

    private void configureMultiStreamOutputs(Object obj, Map<String, OutputMultiFileState> outputs) throws IllegalAccessException, LensfieldException, IOException {
        for (OutputDescription output : this.outputs) {
            if (outputs.containsKey(output.name)) {
                MultiStreamOut out = outputs.get(output.name).getOutput();
                output.field.set(obj, out);
            } else {
                throw new LensfieldException("Missing output file: "+output.name);
            }
        }
    }


    private LensfieldProcess proxy(Object obj) {
        if (obj instanceof LensfieldProcess) {
            return (LensfieldProcess) obj;
        } else {
            return new ProcessProxy(obj, runMethod);
        }
    }


    public void setLogger(Logger log) {
        this.LOG = log;
    }
}