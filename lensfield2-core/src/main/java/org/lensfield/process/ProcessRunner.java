/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.process;

import org.apache.commons.io.FileUtils;
import org.lensfield.DebugClassLoader;
import org.lensfield.InputFileSet;
import org.lensfield.LensfieldException;
import org.lensfield.api.Logger;
import org.lensfield.api.io.StreamIn;
import org.lensfield.api.io.StreamOut;
import org.lensfield.build.FileList;
import org.lensfield.build.InputDescription;
import org.lensfield.build.OutputDescription;
import org.lensfield.build.ParameterDescription;
import org.lensfield.glob.MissingParameterException;
import org.lensfield.io.*;
import org.lensfield.log.BuildLogger;
import org.lensfield.state.FileState;
import org.lensfield.state.TaskState;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author sea36
 */
public class ProcessRunner {

    private File root, tmpdir;

    private TaskState task;
    private Class<?> clazz;
    private Method runMethod;
    private Map<InputDescription,Field> inputs;
    private Map<OutputDescription,Field> outputs;
    private Map<ParameterDescription, Field> parameters;

    private Logger LOG;
    private BuildLogger buildLog;
    private ClassLoader classloader;

    public ProcessRunner(TaskState task) throws Exception {
        this.task = task;
        init();
    }

    private void init() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            this.classloader = task.createClassLoader();
            Thread.currentThread().setContextClassLoader(this.classloader);
            System.err.println("LOADING CLASS: "+task.getClassName());
            this.clazz = classloader.loadClass(task.getClassName());
//            DebugClassLoader.debug(clazz);

            Class<?> runClass = classloader.loadClass(task.getMethodClass());
            runMethod = runClass.getDeclaredMethod(task.getMethodName(), task.getMethodParams());

            inputs = new LinkedHashMap<InputDescription,Field>();
            for (InputDescription input : task.getInputs()) {
                if (task.isNoArgs()) {
                    Class<?> fieldClass = classloader.loadClass(input.getFieldClass());
                    Field field = fieldClass.getDeclaredField(input.getFieldName());
                    inputs.put(input,field);
                } else {
                    inputs.put(input,null);
                }
            }
            outputs = new LinkedHashMap<OutputDescription,Field>();
            for (OutputDescription output : task.getOutputs()) {
                if (task.isNoArgs()) {
                    Class<?> fieldClass = classloader.loadClass(output.getFieldClass());
                    Field field = fieldClass.getDeclaredField(output.getFieldName());
                    outputs.put(output,field);
                } else {
                    outputs.put(output,null);
                }
            }
            parameters = new LinkedHashMap<ParameterDescription,Field>();
            for (ParameterDescription param: task.getParameters()) {
                Class<?> fieldClass = classloader.loadClass(param.getFieldClass());
                Field field = fieldClass.getDeclaredField(param.getFieldName());
                parameters.put(param,field);
            }
            System.err.println("----------");

            checkClassInstantiable();
            ensureFieldsAccessible();
            
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
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
            for (Map.Entry<InputDescription,Field> input : inputs.entrySet()) {
                Field field = input.getValue();
                field.setAccessible(true);
            }
            for (Map.Entry<OutputDescription,Field> output : outputs.entrySet()) {
                Field field = output.getValue();
                field.setAccessible(true);
            }
        }
        for (Map.Entry<ParameterDescription,Field> param : parameters.entrySet()) {
            Field field = param.getValue();
            field.setAccessible(true);
        }
    }


    private void configureParameters(Object obj) throws IllegalAccessException, LensfieldException {
        for (Map.Entry<ParameterDescription,Field> e : parameters.entrySet()) {
            ParameterDescription param = e.getKey();
            Field field = e.getValue();
            if (param.getValue() != null) {
                field.set(obj, param.getValue());
            }
        }
    }


    public void runProcess(InputFileSet inputs, Map<String,FileList> outputs) throws Exception {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classloader);

            Object obj = clazz.newInstance();
            configureParameters(obj);

            List<Input> ins = Collections.emptyList();
            Map<String, Output> outputMap = Collections.emptyMap();
            try {
                if (task.isNoArgs()) {

                    ins = configureInputs(inputs, obj);
                    outputMap = configureOutputs(inputs, outputs, obj);
                    runMethod.invoke(obj);

                } else {
                    FileState fin = inputs.getMap().values().iterator().next().iterator().next();
                    LOG.debug("reading "+fin.getPath());
                    InputFile in = new InputFile(fin.getPath(), new File(root, fin.getPath()), fin.getParams());
                    FileList fileList = outputs.values().iterator().next();
                    OutputFile out = configureOutputFile(inputs, fileList);

                    ins = Collections.<Input>singletonList(in);
                    outputMap = Collections.<String, Output>singletonMap("out", out);

                    runArgsTask(obj, in, out);

                }
            } finally {
                // Ensure all streams are closed
                closeStreams(ins);
                closeStreams(outputMap.values());
            }

            // Move temp files
            renameTempFiles(task.getId(), inputs, outputMap);

            Map<String,List<FileState>> outputFiles = new HashMap<String, List<FileState>>();
            for (Map.Entry<String, Output> e : outputMap.entrySet()) {
                List<FileState> list = new ArrayList<FileState>();
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
                    FileState f = new FileState(out.getPath(), out.getFile().lastModified(), out.getParams());
                    outputs.get(e.getKey()).addFile(f);
                    list.add(f);
                }
                outputFiles.put(e.getKey(), list);
            }

            buildLog.process(task.getId(), inputs.getMap(), outputFiles);

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

    private Map<String, Output> configureOutputs(InputFileSet inputs, Map<String, FileList> outputs, Object obj) throws IllegalAccessException, IOException, LensfieldException {
        Map<String, Output> outputMap;
        outputMap = new HashMap<String, Output>();

        for (Map.Entry<OutputDescription, Field> e : this.outputs.entrySet()) {
            OutputDescription output = e.getKey();
            Field field = e.getValue();
            if (outputs.containsKey(output.getName())) {
                FileList list = outputs.get(output.getName());
                if (output.isMultifile()) {
                    OutputMultiFile out = new OutputMultiFile(tmpdir, list.getGlob(), inputs.getParameters());
                    outputMap.put(output.getName(), out);
                    field.set(obj, out);
                } else {
                    OutputFile out = configureOutputFile(inputs, list);
                    outputMap.put(output.getName(), out);
                    field.set(obj, out);
                }
            } else {
                throw new LensfieldException("Missing output file: "+output.getName());
            }
        }
        return outputMap;
    }

    private List<Input> configureInputs(InputFileSet inputs, Object obj) throws IOException, IllegalAccessException, LensfieldException {
        List<Input> ins;
        ins = new ArrayList<Input>();
        Map<String,Collection<FileState>> inputMap = inputs.getMap();
        for (Map.Entry<InputDescription, Field> e : this.inputs.entrySet()) {
            InputDescription input = e.getKey();
            Field field = e.getValue();
            if (inputMap.containsKey(input.getName())) {
                Collection<FileState> list = inputMap.get(input.getName());
                if (input.isMultifile()) {
                    List<InputFile> inputFiles = new ArrayList<InputFile>(list.size());
                    int i = 1;
                    for (FileState fs : list) {
                        InputFile in = createInput(fs);
                        inputFiles.add(in);
                    }
                    InputMultiFile in = new InputMultiFile(inputFiles, LOG);
                    field.set(obj, in);
                    ins.add(in);
                } else {
                    if (list.size() != 1) {
                        throw new LensfieldException("Single file input required");
                    }
                    FileState fs = list.iterator().next();
                    InputFile in = createInput(fs);
                    LOG.debug("reading "+in.getPath());
                    field.set(obj, in);
                    ins.add(in);
                }
            } else {
                throw new LensfieldException("Missing input file: "+input.getName());
            }
        }
        return ins;
    }

    private InputFile createInput(FileState fs) throws IOException {
        String path = fs.getPath();
        File file = new File(root, path);
        Map<String,String> params = fs.getParams();
        InputFile in = new InputFile(path, file, params);
        return in;
    }

    private OutputFile configureOutputFile(InputFileSet inputs, FileList fileList) throws IOException {
        File tmpFile = new File(tmpdir, UUID.randomUUID().toString());
        Map<String,String> params = new HashMap<String,String>(inputs.getParameters());
        return new OutputFile(tmpFile, params, fileList.getGlob());
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



    private void renameTempFiles(String name, InputFileSet inputs, Map<String, Output> outputFiles) throws LensfieldException, IOException {
        for (Map.Entry<String,Output> e : outputFiles.entrySet()) {
            Output out = e.getValue();
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
                    for (Map.Entry<String,Collection<FileState>> entry : inputs.getMap().entrySet()) {
                        System.err.println("--- Input: "+entry.getKey());
                        for (FileState f : entry.getValue()) {
                            System.err.println(f.getPath());
                        }
                    }
                    System.err.println("--- Output ---");
                    System.err.println("Glob: "+output.getGlob().getGlob());
                    System.err.println("Parameters: "+output.getParams());
                    throw new LensfieldException("Failed to create output file", ex);
                }
                // TODO check for duplicate output paths
                FileState fr = new FileState(path, tempFile.lastModified(), output.getParams());
                File file = new File(root, fr.getPath());
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
                    LOG.debug(name, "unchanged "+path);
                } else {
                    LOG.debug(name, "writing "+path);
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

    public void setLogger(Logger log) {
        this.LOG = log;
    }

    public void setBuildLog(BuildLogger buildLog) {
        this.buildLog = buildLog;
    }

}