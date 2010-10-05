/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.concurrent;

import org.apache.commons.io.FileUtils;
import org.lensfield.LensfieldException;
import org.lensfield.api.io.StreamIn;
import org.lensfield.api.io.StreamOut;
import org.lensfield.glob.MissingParameterException;
import org.lensfield.io.InputFile;
import org.lensfield.io.InputMultiFile;
import org.lensfield.io.OutputFile;
import org.lensfield.io.OutputMultiFile;
import org.lensfield.state.Input;
import org.lensfield.state.Operation;
import org.lensfield.state.Output;
import org.lensfield.state.Parameter;
import org.lensfield.state.Process;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author sea36
 */
public class ProcessRunner {

    private File root, tmpdir;

    private Process task;
    private Class<?> clazz;
    private Method runMethod;
    private Map<Input,Field> inputs;
    private Map<Output,Field> outputs;
    private Map<Parameter, Field> parameters;

//    private BuildLogger buildLog;
    private ClassLoader classloader;

    public ProcessRunner(Process task) throws Exception {
        this.task = task;
        init();
    }

    private void init() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            this.classloader = task.createClassLoader();
            Thread.currentThread().setContextClassLoader(this.classloader);
            this.clazz = classloader.loadClass(task.getClassName());

            Class<?> runClass = classloader.loadClass(task.getMethodClass());
            runMethod = runClass.getDeclaredMethod(task.getMethodName(), task.getMethodParams());

            inputs = getInputMap();
            outputs = getOutputMap();
            parameters = getParameterMap();

            checkClassInstantiable();
            ensureFieldsAccessible();

        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private Map<Parameter, Field> getParameterMap() throws NoSuchFieldException, ClassNotFoundException {
        Map<Parameter, Field> parameters = new LinkedHashMap<Parameter,Field>();
        for (Parameter param: task.getParameters()) {
            Class<?> fieldClass = classloader.loadClass(param.getFieldClass());
            Field field = fieldClass.getDeclaredField(param.getFieldName());
            parameters.put(param,field);
        }
        return parameters;
    }

    private Map<Output, Field> getOutputMap() throws NoSuchFieldException, ClassNotFoundException {
        Map<Output, Field> outputs = new LinkedHashMap<Output,Field>();
        for (Output output : task.getOutputs()) {
            if (task.isNoArgs()) {
                Class<?> fieldClass = classloader.loadClass(output.getFieldClass());
                Field field = fieldClass.getDeclaredField(output.getFieldName());
                outputs.put(output,field);
            } else {
                outputs.put(output,null);
            }
        }
        return outputs;
    }

    private Map<Input, Field> getInputMap() throws NoSuchFieldException, ClassNotFoundException {
        Map<Input,Field> inputs = new LinkedHashMap<org.lensfield.state.Input,Field>();
        for (Input input : task.getInputs()) {
            if (task.isNoArgs()) {
                Class<?> fieldClass = classloader.loadClass(input.getFieldClass());
                Field field = fieldClass.getDeclaredField(input.getFieldName());
                inputs.put(input,field);
            } else {
                inputs.put(input,null);
            }
        }
        return inputs;
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
            for (Map.Entry<Input,Field> input : inputs.entrySet()) {
                Field field = input.getValue();
                field.setAccessible(true);
            }
            for (Map.Entry<Output,Field> output : outputs.entrySet()) {
                Field field = output.getValue();
                field.setAccessible(true);
            }
        }
        for (Map.Entry<Parameter,Field> param : parameters.entrySet()) {
            Field field = param.getValue();
            field.setAccessible(true);
        }
    }


    private void configureParameters(Object obj) throws IllegalAccessException, LensfieldException {
        for (Map.Entry<Parameter,Field> e : parameters.entrySet()) {
            Parameter param = e.getKey();
            Field field = e.getValue();
            if (param.getValue() != null) {
                field.set(obj, param.getValue());
            }
        }
    }


    public void runProcess(Operation task) throws Exception {
        runProcess(task.getInputFiles(), task.getOutputFiles(), task.getParameters());
    }


    private void runProcess(Map<Input,ResourceSet> inputs, Set<Output> outputs, Map<String,String> parameters) throws Exception {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classloader);

            Object obj = clazz.newInstance();
            configureParameters(obj);

            List<org.lensfield.io.Input> ins = Collections.emptyList();
            Map<Output, org.lensfield.io.Output> outputMap = Collections.emptyMap();
            try {
                if (task.isNoArgs()) {
                    ins = configureInputs(inputs, obj);
                    outputMap = configureOutputs(parameters, outputs, obj);
                    runMethod.invoke(obj);
                } else {
                    if (inputs.size() != 1 || outputs.size() != 1) {
                        throw new IllegalStateException();
                    }
                    Resource fin = inputs.values().iterator().next().list().get(0);
                    InputFile in = new InputFile(fin.getPath(), new File(root, fin.getPath()), fin.getParameters());
                    OutputFile out = configureOutputFile(parameters, outputs.iterator().next());

                    ins = Collections.<org.lensfield.io.Input>singletonList(in);
                    outputMap = Collections.<Output,org.lensfield.io.Output>singletonMap(outputs.iterator().next(), out);

                    runArgsTask(obj, in, out);
                }
            } finally {
                // Ensure all streams are closed
                closeStreams(ins);
                closeStreams(outputMap.values());
            }

            // Move temp files
            renameTempFiles(task.getId(), inputs, outputMap);

            for (Map.Entry<Output,org.lensfield.io.Output> e : outputMap.entrySet()) {
                List<Resource> list = new ArrayList<Resource>();
                List<OutputFile> outs;
                if (e.getValue() instanceof OutputFile) {
                    outs = Collections.singletonList((OutputFile)e.getValue());
                }
                else if (e.getValue() instanceof OutputMultiFile) {
                    outs = ((OutputMultiFile)e.getValue()).getOutputs();
                }
                else {
                    throw new RuntimeException();
                }
                for (OutputFile out : outs) {
                    Resource f = new Resource(out.getPath(), out.getFile(), out.getParams());
                    e.getKey().addResource(f);
                    list.add(f);
                }
            }

//            buildLog.process(task.getId(), inputs.getMap(), outputFiles);

        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

    }

    private void closeStreams(Collection<? extends Closeable> cs) {
        for (Closeable c : cs) {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<Output, org.lensfield.io.Output> configureOutputs(Map<String,String> params, Set<Output> outputs, Object obj) throws IllegalAccessException, IOException, LensfieldException {

        Map<Output, org.lensfield.io.Output> outputMap = new HashMap<Output, org.lensfield.io.Output>();
        for (Map.Entry<Output, Field> e : this.outputs.entrySet()) {
            Output output = e.getKey();
            Field field = e.getValue();
            if (outputs.contains(output)) {
                if (output.isMultifile()) {
                    OutputMultiFile out = new OutputMultiFile(tmpdir, output.getGlob(), params);
                    outputMap.put(output, out);
                    field.set(obj, out);
                } else {
                    OutputFile out = configureOutputFile(params,output);
                    outputMap.put(output, out);
                    field.set(obj, out);
                }
            } else {
                throw new LensfieldException("Missing output file: "+output.getName());
            }
        }
        return outputMap;
    }


    private List<org.lensfield.io.Input> configureInputs(Map<Input, ResourceSet> inputs, Object obj) throws IOException, IllegalAccessException, LensfieldException {
        List<org.lensfield.io.Input> ins = new ArrayList<org.lensfield.io.Input>();
        for (Map.Entry<Input, Field> e : this.inputs.entrySet()) {
            Input input = e.getKey();
            Field field = e.getValue();
            List<Resource> list = inputs.get(input).list();
            if (input.isMultifile()) {
                List<InputFile> inputFiles = new ArrayList<InputFile>(list.size());
                int i = 1;
                for (Resource fs : list) {
                    InputFile in = createInput(fs);
                    inputFiles.add(in);
                }
                InputMultiFile in = new InputMultiFile(inputFiles, null);
                field.set(obj, in);
                ins.add(in);
            } else {
                if (list.size() != 1) {
                    throw new LensfieldException("Single file input required");
                }
                Resource fs = list.iterator().next();
                InputFile in = createInput(fs);
//                LOG.debug("reading "+in.getPath());
                field.set(obj, in);
                ins.add(in);
            }
        }
        return ins;
    }

    private InputFile createInput(Resource fs) throws IOException {
        String path = fs.getPath();
        File file = new File(root, path);
        Map<String,String> params = fs.getParameters();
        InputFile in = new InputFile(path, file, params);
        return in;
    }

    private OutputFile configureOutputFile(Map<String,String> parameters, Output output) throws IOException {
        File tmpFile = new File(tmpdir, UUID.randomUUID().toString());
        Map<String,String> params = new HashMap<String,String>(parameters);
        return new OutputFile(tmpFile, params, output.getGlob());
    }

    private void runArgsTask(Object obj, StreamIn in, StreamOut out) throws Exception {
        try {
            runMethod.invoke(obj, in, out);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void renameTempFiles(String name, Map<Input, ResourceSet> inputs, Map<Output, org.lensfield.io.Output> outputFiles) throws LensfieldException, IOException {
        for (Map.Entry<Output, org.lensfield.io.Output> e : outputFiles.entrySet()) {
            org.lensfield.io.Output out = e.getValue();
            List<OutputFile> outputs;
            if (out instanceof OutputFile) {
                outputs = Collections.singletonList((OutputFile)out);
            }
            else if (out instanceof OutputMultiFile) {
                outputs = ((OutputMultiFile)out).getOutputs();
            }
            else {
                throw new RuntimeException();
            }
            for (OutputFile output : outputs) {
                File tempFile = output.getFile();
                // TODO handle missing glob parameters
                String path = null;
                try {
                    path = output.getGlob().format(output.getParams());
                } catch (MissingParameterException ex) {
                    System.err.println("Missing parameter: "+ex.getName());
                    for (Map.Entry<Input,ResourceSet> entry : inputs.entrySet()) {
                        System.err.println("--- Input: "+entry.getKey().getName());
                        for (Resource f : entry.getValue().list()) {
                            System.err.println(f.getPath());
                        }
                    }
                    System.err.println("--- Output ---");
                    System.err.println("Glob: "+output.getGlob().getGlob());
                    System.err.println("Parameters: "+output.getParams());
                    throw new LensfieldException("Failed to create output file", ex);
                }
                // TODO check for duplicate output paths
                File file = new File(root, path);
                Resource fr = new Resource(path, file, output.getParams());
                File parent = file.getParentFile();
                if (!parent.isDirectory()) {
                    if (!parent.mkdirs()) {
                        throw new LensfieldException("Unable to create output directory "+parent);
                    }
                }
                if (file.isFile()) {
                    if (!file.delete()) {
                        throw new LensfieldException("Unable to delete file "+file);
                    }
                }
                if (file.isFile() && isUnchanged(tempFile, file)) {
//                    LOG.debug(name, "unchanged "+path);
                } else {
//                    LOG.debug(name, "writing "+path);
                    if (!tempFile.renameTo(file)) {
                        throw new LensfieldException("Unable to rename file "+tempFile+" to "+file);
                    }
                }
                output.setPath(path);
                output.setFile(file);

            }
        }
    }

    private boolean isUnchanged(File f1, File f2) throws IOException {
        if (f1.length() != f2.length()) {
            return false;
        }
        return FileUtils.contentEquals(f1, f2);
    }

    public void setRoot(File root) {
        this.root = root;
    }

    public void setTmpdir(File tmpdir) {
        this.tmpdir = tmpdir;
    }

//    public void setBuildLog(BuildLogger buildLog) {
//        this.buildLog = buildLog;
//    }

}