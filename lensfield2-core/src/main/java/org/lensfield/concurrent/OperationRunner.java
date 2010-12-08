/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.concurrent;

import org.apache.commons.io.FileUtils;
import org.lensfield.LensfieldException;
import org.lensfield.api.io.StreamIn;
import org.lensfield.api.io.StreamOut;
import org.lensfield.glob.MissingParameterException;
import org.lensfield.io.*;
import org.lensfield.log.BuildLogger;
import org.lensfield.state.*;
import org.lensfield.state.InputPipe;
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
public class OperationRunner {

    private File root, tmpdir;

    private Process task;
    private Class<?> clazz;
    private Method runMethod;
    private Map<InputPipe,Field> inputs;
    private Map<OutputPipe,Field> outputs;
    private Map<Parameter, Field> parameters;

    private BuildLogger buildLogger;
    private ClassLoader classloader;

    public OperationRunner(Process task, BuildLogger buildLogger) throws Exception {
        this.task = task;
        this.buildLogger = buildLogger;
        init();
    }

    private void init() throws Exception {
        ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
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
            Thread.currentThread().setContextClassLoader(oldClassloader);
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

    private Map<OutputPipe, Field> getOutputMap() throws NoSuchFieldException, ClassNotFoundException {
        Map<OutputPipe, Field> outputs = new LinkedHashMap<OutputPipe,Field>();
        for (OutputPipe output : task.getOutputs()) {
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

    private Map<InputPipe, Field> getInputMap() throws NoSuchFieldException, ClassNotFoundException {
        Map<InputPipe,Field> inputs = new LinkedHashMap<InputPipe,Field>();
        for (InputPipe input : task.getInputs()) {
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
            for (Map.Entry<InputPipe,Field> input : inputs.entrySet()) {
                Field field = input.getValue();
                field.setAccessible(true);
            }
            for (Map.Entry<OutputPipe,Field> output : outputs.entrySet()) {
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


    public void runProcess(Operation operation) throws Exception {

        Map<InputPipe,ResourceSet> inputs = operation.getInputResourcesMap();
        Set<OutputPipe> outputs = operation.getOutputSet();
        Map<String,String> parameters = operation.getParameters();

        ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classloader);

            Map<OutputPipe, Output> outputMap = invokeOperation(inputs, outputs, parameters);

            // Move temp files
            Map<OutputPipe,List<Resource>> outputResourcesMap = renameTempFiles(inputs, outputMap);

            buildLogger.logOperation(operation, outputResourcesMap);

            pipeOutputResources(outputResourcesMap);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }

    }

    private void pipeOutputResources(Map<OutputPipe, List<Resource>> outputResourcesMap) {
        for (Map.Entry<OutputPipe,List<Resource>> e : outputResourcesMap.entrySet()) {
            OutputPipe pipe = e.getKey();
            List<Resource> resourceList = e.getValue();
            for (Resource resource : resourceList) {
                pipe.sendResource(resource);
            }
        }
    }

    private Map<OutputPipe, Output> invokeOperation(Map<InputPipe, ResourceSet> inputs, Set<OutputPipe> outputs, Map<String, String> parameters) throws Exception {
        Object obj = clazz.newInstance();
        configureParameters(obj);
        
        List<Input> inputList = Collections.emptyList();
        Map<OutputPipe, Output> outputMap = Collections.emptyMap();
        try {
            if (task.isNoArgs()) {
                inputList = configureInputs(inputs, obj);
                outputMap = configureOutputs(parameters, outputs, obj);
                runMethod.invoke(obj);
            } else {
                if (inputs.size() != 1 || outputs.size() != 1) {
                    throw new IllegalStateException();
                }
                Resource fin = inputs.values().iterator().next().getResourceList().get(0);
                InputFile in = new InputFile(fin.getPath(), new File(root, fin.getPath()), fin.getParameters());
                OutputFile out = configureOutputFile(parameters, outputs.iterator().next());

                inputList = Collections.<Input>singletonList(in);
                outputMap = Collections.<OutputPipe, Output>singletonMap(outputs.iterator().next(), out);

                runArgsTask(obj, in, out);
            }
        } finally {
            // Ensure all streams are closed
            closeStreams(inputList);
            closeStreams(outputMap.values());
        }
        return outputMap;
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

    private Map<OutputPipe, org.lensfield.io.Output> configureOutputs(Map<String,String> params, Set<OutputPipe> outputs, Object obj) throws IllegalAccessException, IOException, LensfieldException {

        Map<OutputPipe, org.lensfield.io.Output> outputMap = new HashMap<OutputPipe, org.lensfield.io.Output>();
        for (Map.Entry<OutputPipe, Field> e : this.outputs.entrySet()) {
            OutputPipe output = e.getKey();
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


    private List<org.lensfield.io.Input> configureInputs(Map<InputPipe, ResourceSet> inputs, Object obj) throws IOException, IllegalAccessException, LensfieldException {
        List<org.lensfield.io.Input> ins = new ArrayList<org.lensfield.io.Input>();
        for (Map.Entry<InputPipe, Field> e : this.inputs.entrySet()) {
            InputPipe input = e.getKey();
            Field field = e.getValue();
            List<Resource> list = inputs.get(input).getResourceList();
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

    private OutputFile configureOutputFile(Map<String,String> parameters, OutputPipe output) throws IOException {
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



    private Map<OutputPipe, List<Resource>> renameTempFiles(Map<InputPipe, ResourceSet> inputs, Map<OutputPipe, Output> outputFiles) throws LensfieldException, IOException {

        ResourceManager resourceManager = task.getReactor().getResourceManager();

        Map<OutputPipe,List<Resource>> outputResourcesMap = new HashMap<OutputPipe,List<Resource>>();

        for (Map.Entry<OutputPipe, org.lensfield.io.Output> e : outputFiles.entrySet()) {
            OutputPipe outputPipe = e.getKey();
            org.lensfield.io.Output out = e.getValue();

            List<OutputFile> outputFileList;
            if (out instanceof OutputFile) {
                outputFileList = Collections.singletonList((OutputFile)out);
            }
            else if (out instanceof OutputMultiFile) {
                outputFileList = ((OutputMultiFile)out).getOutputs();
            }
            else {
                throw new RuntimeException("Unexpected output: "+out.getClass());
            }

            List<Resource> resourceList = new ArrayList<Resource>(outputFileList.size());

            for (OutputFile outputFile : outputFileList) {
                File tempFile = outputFile.getFile();
                // TODO handle missing glob parameters
                String resourcePath = null;
                try {
                    resourcePath = outputFile.getGlob().format(outputFile.getParams());
                } catch (MissingParameterException ex) {
                    System.err.println("Missing parameter: "+ex.getName());
                    for (Map.Entry<InputPipe,ResourceSet> entry : inputs.entrySet()) {
                        System.err.println("--- InputPipe: "+entry.getKey().getName());
                        for (Resource f : entry.getValue().getResourceList()) {
                            System.err.println(f.getPath());
                        }
                    }
                    System.err.println("--- OutputPipe ---");
                    System.err.println("Glob: "+ outputFile.getGlob().getGlob());
                    System.err.println("Parameters: "+ outputFile.getParams());
                    throw new LensfieldException("Failed to create output file", ex);
                }
                // TODO check for duplicate output paths
                File resourceFile = new File(root, resourcePath);
                Resource resource = new Resource(resourcePath, resourceFile, outputFile.getParams());
                resourceManager.addResource(resource);
                resourceList.add(resource);

                File parent = resourceFile.getParentFile();
                if (!parent.isDirectory()) {
                    if (!parent.mkdirs()) {
                        throw new LensfieldException("Unable to create output directory "+parent);
                    }
                }

                boolean write = true;
                if (resourceFile.isFile()) {
                    if (isUnchanged(tempFile, resourceFile)) {
//                        LOG.debug(name, "unchanged "+resourcePath);
                        write = false;
                    } else {
                        if (!resourceFile.delete()) {
                            throw new LensfieldException("Unable to delete file "+ resourceFile);
                        }
                    }
                }
                if (write) {
//                    LOG.debug(name, "writing "+resourcePath);
                    if (!tempFile.renameTo(resourceFile)) {
                        throw new LensfieldException("Unable to rename file "+tempFile+" to "+ resourceFile);
                    }
                }
                outputFile.setPath(resourcePath);
                outputFile.setFile(resourceFile);
            }

            outputResourcesMap.put(outputPipe, resourceList);
        }

        return outputResourcesMap;
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