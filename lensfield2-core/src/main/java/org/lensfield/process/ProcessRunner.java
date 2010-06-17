/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.process;

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
import org.lensfield.glob.Template;
import org.lensfield.io.*;
import org.lensfield.log.BuildLogger;
import org.lensfield.state.FileState;
import org.lensfield.state.TaskState;

import java.io.File;
import java.io.IOException;
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
    private List<InputDescription> inputs;
    private List<OutputDescription> outputs;
    private List<ParameterDescription> parameters;

    private Logger LOG;
    private BuildLogger buildLog;

    public ProcessRunner(TaskState task) throws LensfieldException {
        this.task = task;
        inputs = new ArrayList<InputDescription>(task.getInputs());
        outputs = new ArrayList<OutputDescription>(task.getOutputs());
        parameters = new ArrayList<ParameterDescription>(task.getParameters());
        clazz = task.getClazz();

        runMethod = task.getMethod();

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
                input.getField().setAccessible(true);
            }
            for (OutputDescription output : outputs) {
                output.getField().setAccessible(true);
            }
        }
        for (ParameterDescription param : parameters) {
            param.field.setAccessible(true);
        }
    }


    private void configureParameters(Object obj) throws IllegalAccessException, LensfieldException {
        for (ParameterDescription param : this.parameters) {
            if (param.value != null) {
                param.field.set(obj, param.value);
            }
        }
    }


    public void runProcess(InputFileSet inputs, Map<String,FileList> outputs) throws Exception {

        Object obj = clazz.newInstance();
        configureParameters(obj);

        List<Input> ins = Collections.emptyList();
        Map<String, Output> outputMap = Collections.emptyMap();
        try {
            if (task.isNoArgs()) {

                ins = new ArrayList<Input>();
                Map<String,List<FileState>> inputMap = inputs.getMap();
                for (InputDescription input : this.inputs) {
                    if (inputMap.containsKey(input.getName())) {
                        List<FileState> list = inputMap.get(input.getName());
                        if (input.isMultifile()) {
                            List<InputFile> inputFiles = new ArrayList<InputFile>(list.size());
                            int i = 1;
                            for (FileState fs : list) {
                                InputFile in = createInput(fs);
                                inputFiles.add(in);
                            }
                            InputMultiFile in = new InputMultiFile(inputFiles, LOG);
                            input.getField().set(obj, in);
                            ins.add(in);                                                        
                        } else {
                            if (list.size() != 1) {
                                throw new LensfieldException("Single file input required");
                            }
                            FileState fs = list.get(0);
                            InputFile in = createInput(fs);
                            LOG.debug("reading "+in.getPath());
                            input.getField().set(obj, in);
                            ins.add(in);
                        }
                    } else {
                        throw new LensfieldException("Missing input file: "+input.getName());
                    }
                }

                outputMap = new HashMap<String, Output>();

                for (OutputDescription output : this.outputs) {
                    if (outputs.containsKey(output.getName())) {
                        FileList list = outputs.get(output.getName());
                        if (output.isMultifile()) {
                            OutputMultiFile out = new OutputMultiFile(tmpdir, list.getGlob(), inputs.getParameters());
                            outputMap.put(output.getName(), out);
                            output.getField().set(obj, out);
                        } else {
                            OutputFile out = configureOutputFile(inputs, list);
                            outputMap.put(output.getName(), out);
                            output.getField().set(obj, out);
                        }
                    } else {
                        throw new LensfieldException("Missing output file: "+output.getName());
                    }
                }

                runMethod.invoke(obj);

            } else {
                FileState fin = inputs.getMap().values().iterator().next().get(0);
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
            for (Input in : ins) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            for (Output out : outputMap.values()) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Move temp files
        renameTempFiles(task.getId(), inputs, outputMap);

        Map<String,List<FileState>> outputFiles = new HashMap<String, List<FileState>>();
        for (Map.Entry<String, Output> e : outputMap.entrySet()) {
            Template glob = task.getOutput(e.getKey()).getGlob();
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
                FileState f = new FileState(glob, out.getPath(), out.getFile().lastModified(), out.getParams());
                outputs.get(e.getKey()).addFile(f);
                list.add(f);
            }
            outputFiles.put(e.getKey(), list);
        }

        buildLog.process(task.getId(), inputs.getMap(), outputFiles);

    }

    private InputFile createInput(FileState fs) throws IOException {
        String path = fs.getPath();
        File file = new File(root, path);
        Map<String,String> params = fs.getParams();
        if (params == null) {
            // todo
//            params = task.getOutput(outputName).glob.matches(fs.getPath());
            fs.setParams(params);
        }
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



    private void renameTempFiles(String name, InputFileSet inputs, Map<String, Output> outputFiles) throws LensfieldException {
        for (Map.Entry<String,Output> e : outputFiles.entrySet()) {
            Output out = e.getValue();
            Template glob = task.getOutput(e.getKey()).getGlob();
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
                    for (Map.Entry<String,List<FileState>> entry : inputs.getMap().entrySet()) {
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
                FileState fr = new FileState(glob, path, tempFile.lastModified(), output.getParams());
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
                if (!tempFile.renameTo(file)) {
                    throw new LensfieldException("Unable to rename file "+tempFile+" to "+file);
                }
                output.setPath(path);
                output.setFile(file);
                LOG.debug(name, "writing "+path);
            }
        }
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