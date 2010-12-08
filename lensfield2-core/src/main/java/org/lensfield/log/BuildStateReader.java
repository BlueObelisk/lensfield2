/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.log;

import org.apache.log4j.Logger;
import org.lensfield.concurrent.Resource;
import org.lensfield.state.*;
import org.lensfield.state.Process;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class BuildStateReader {

    private static final Logger LOG = Logger.getLogger(BuildStateReader.class);

    private static final int EOF = -1;

    private BuildState build;

    private Reader in;
    private int ch;


    public synchronized BuildState parseBuildState(Reader in) throws IOException, ParseException {
        this.in = in;
        this.ch = in.read();

        this.build = new BuildState();

        while (ch != EOF) {
            skipWhitespace();
            if (ch != '(') {
                throw new IOException("Expected command");
            }
            ch = in.read();
            skipWhitespace();

            String cmd = readUnquotedToken();
            skipWhitespace();
            if ("build-started".equals(cmd)) {
                readBuildStarted();
            }
            else if ("task".equals(cmd)) {
                readTask();
            }
            else if ("source".equals(cmd)) {
                readSource();
            }
            else if ("op".equals(cmd)) {
                readOp();
            }
            else if ("build-finished".equals(cmd)) {
                readToken();
            }
            else {
                throw new IOException("Expected command");
            }

            skipWhitespace();
            if (ch != ')') {
                throw new IOException("Expected closing ')'");
            }

            ch = in.read();
            skipWhitespace();
        }

        return build;
    }

    private void readOp() throws IOException, ParseException {
        String taskName = readToken();
        org.lensfield.state.Process task = build.getTask(taskName);

        // Read inputs
        skipWhitespace();
        if (ch != '(') {
            throw new IOException();
        }
        ch = in.read();
        skipWhitespace();

        Map<String,List<Resource>> inputFiles = null;
        if (ch == ')') {
            inputFiles = Collections.emptyMap();
        } else {
            boolean first = true;
            String inpname = readToken();
            skipWhitespace();
            do {
                List<Resource> inpfiles = Collections.emptyList();
                // TODO
                if (ch == '(') {
                    ch = in.read();
                    skipWhitespace();
                    List<String> inpFileNames = readFileNameList();
                    inpfiles = new ArrayList<Resource>(inpFileNames.size());
                    for (String path : inpFileNames) {
//                        inpfiles.addResource(build.getOutputSet().get(path));
                    }
                } else {
                    String path = readToken();
//                    inpfiles = Collections.singletonList(build.getOutputSet().get(path));
                }
                skipWhitespace();
                if (first) {
                    if (ch == ')') {
                        inputFiles = Collections.singletonMap(inpname,inpfiles);
                        break;
                    } else {
                        inputFiles = new HashMap<String, List<Resource>>();
                    }
                    first = false;
                }
                inputFiles.put(inpname, inpfiles);
            } while (ch != ')');
        }
        ch = in.read();


        // Read outputs
        skipWhitespace();
        if (ch != '(') {
            throw new IOException();
        }
        ch = in.read();
        skipWhitespace();

        Map<String,List<Resource>> outputFiles = null;
        if (ch == ')') {
            outputFiles = Collections.emptyMap();
        } else {
            boolean first = true;
            do {
                String outpname = readToken();
                skipWhitespace();
                List<Resource> outpfiles;
                if (ch == '(') {
                    ch = in.read();
                    skipWhitespace();
                    outpfiles = readFileList();
                } else {
                    if (ch != '[') {
                        throw new IOException();
                    }
                    ch = in.read();
                    outpfiles = Collections.singletonList(readResource());
                }
                skipWhitespace();
                if (first) {
                    if (ch != ')') {
                        outputFiles = Collections.singletonMap(outpname,outpfiles);
                        break;
                    } else {
                        outputFiles = new HashMap<String, List<Resource>>();
                    }
                    first = false;
                }
                outputFiles.put(outpname, outpfiles);
            } while (ch != ')');
        }
        ch = in.read();
        skipWhitespace();

        // TODO

//        Operation op = new Operation(inputFiles, outputFiles);
//        task.addOperation(op);
//        for (List<Resource> files : op.getOutputSet().values()) {
//            build.addFiles(files);
//        }
    }

    private List<String> readFileNameList() throws IOException {
        List<String> files = new ArrayList<String>();
        while (ch != ')') {
            if (ch == EOF) {
                throw new EOFException();
            }
            String file = readToken();
            files.add(file);
            skipWhitespace();
        }
        ch = in.read();
        return files;
    }

    private List<Resource> readFileList() throws IOException, ParseException {
        List<Resource> files = new ArrayList<Resource>();
        while (ch != ')') {
            if (ch == EOF) {
                throw new EOFException();
            }
            if (ch != '[') {
                throw new IOException();
            }
            ch = in.read();
            files.add(readResource());
            skipWhitespace();
        }
        ch = in.read();
        return files;
    }

    private void readSource() throws IOException, ParseException {
        String name = readToken();
        Process task = new Process(name, null);
        skipWhitespace();
        if (ch != '(') {
            throw new IOException("Error! ["+((char)ch)+']');
        }
        ch = in.read();
        skipWhitespace();
        List<Resource> files = new ArrayList<Resource>();
        while (ch != ')') {
            if (ch != '[') {
                throw new IOException();
            }
            ch = in.read();
            files.add(readResource());
            skipWhitespace();
        }
        ch = in.read();
        build.addTask(task);
//        build.addFiles(files);    TODO
    }

    private Resource readResource() throws IOException, ParseException {
        skipWhitespace();
        String path = readToken();
        skipWhitespace();
        long lastModified = BuildLogger.DATE_FORMAT.parse(readToken()).getTime();
        skipWhitespace();
        Map<String,String> params = new HashMap<String, String>();
        while (ch != ']') {
            String param = readToken();
            skipWhitespace();
            String value = readToken();
            skipWhitespace();
            params.put(param, value);
        }
        ch = in.read();
        return new Resource(path, lastModified, params);
    }

    private void readTask() throws IOException, ParseException {
        // Read task definition
        String name = readToken();
        skipWhitespace();
        String clazz = readToken();
        LOG.info("Reading task: "+name+" / "+clazz);
        skipWhitespace();
        String timestamp = readToken();
        skipWhitespace();

        Process task = new Process(name, null);
        task.setClassName(clazz);
        task.setLastModified(BuildLogger.DATE_FORMAT.parse(timestamp).getTime());

        // Read dependencies
        while (ch == '(') {
            ch = in.read();
            String cmd = readUnquotedToken();
            if ("depends".equals(cmd)) {
                readDependencies(task);
            }
            else if ("params".equals(cmd)) {
                readParameters(task);
            }
            else {
                throw new IOException("Unknown command: "+cmd);
            }

            if (ch != ')') {
                throw new IOException("Expected closing ')'");
            }
            ch = in.read();
            skipWhitespace();
        }
        build.addTask(task);
    }

    private void readParameters(Process task) throws IOException {
        skipWhitespace();
        while (ch != ')') {
            if (ch == EOF) {
                throw new EOFException();
            }
            if (ch != '[') {
                throw new IOException();
            }
            ch = in.read();
            readParameter(task);
            skipWhitespace();
        }
    }

    private void readParameter(Process task) throws IOException {
        skipWhitespace();
        String name = readToken();
        skipWhitespace();
        String value =  readToken();
        skipWhitespace();
        if (ch != ']') {
            throw new IOException("Expected: ']'; found: '"+((char)ch)+"'");
        }
        ch = in.read();
        task.addParameter(new Parameter(name, value));
    }

    private void readDependencies(Process task) throws IOException, ParseException {
        skipWhitespace();
        while (ch != ')') {
            if (ch == EOF) {
                throw new EOFException();
            }
            if (ch != '[') {
                throw new IOException();
            }
            ch = in.read();
            readDependency(task);
            skipWhitespace();
        }
    }

    private void readDependency(Process task) throws IOException, ParseException {
        skipWhitespace();
        String id = readToken();
        skipWhitespace();
        String ts =  readToken();
        skipWhitespace();
        if (ch != ']') {
            throw new IOException("Expected: ']'; found: '"+((char)ch)+"'");
        }
        ch = in.read();
        task.addDependency(new Dependency(id, BuildLogger.DATE_FORMAT.parse(ts).getTime()));
    }

    private void readBuildStarted() throws IOException, ParseException {
        String timestamp = readToken();
        Date date = BuildLogger.DATE_FORMAT.parse(timestamp);
        build.setTimeStarted(date.getTime());
    }


    private String readToken() throws IOException {
        skipWhitespace();
        String tok;
        if (ch == '"' || ch == '\'') {
            tok = readQuotedToken();
        } else {
            tok = readUnquotedToken();
        }
        return tok;
    }

    private String readUnquotedToken() throws IOException {
        StringBuilder s = new StringBuilder();
        while (ch != EOF && !Character.isWhitespace((char)ch) && ch != '(' && ch != ')' && ch != '[' && ch != ']') {
            s.append((char)ch);
            ch = in.read();
        }
        if (s.length() == 0) {
            throw new IOException("No token!");
        }
        return s.toString();
    }

    private String readQuotedToken() throws IOException {
        // Capture quote
        char quot = (char) ch;

        StringBuilder s = new StringBuilder();
        for (ch = in.read(); ch != quot; ch = in.read()) {
            if (ch == EOF) {
                throw new EOFException();
            }
            if (ch == '\\') {
                ch = in.read();
                if (ch == EOF) {
                    throw new EOFException();
                }
                if (ch == '\r' || ch == '\n') {
                    throw new IOException();
                }
            }
            s.append((char)ch);
        }
        // Advance to next character
        ch = in.read();
        return s.toString();
    }

    private void skipWhitespace() throws IOException {
        while (ch != EOF && Character.isWhitespace((char)ch)) {
            ch = in.read();
        }
    }

}
