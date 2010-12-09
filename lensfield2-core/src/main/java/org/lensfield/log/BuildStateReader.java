/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.log;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.lensfield.LensfieldException;
import org.lensfield.concurrent.Resource;
import org.lensfield.state.Dependency;
import org.lensfield.state.Parameter;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class BuildStateReader {

    private static final Logger LOG = Logger.getLogger(BuildStateReader.class);

    private static final int EOF = -1;

    private BuildLog build;

    private Reader in;
    private int ch;


    public synchronized BuildLog parseBuildState(Reader in) throws IOException, LensfieldException {
        this.in = in;
        this.ch = in.read();

        this.build = new BuildLog();

        while (ch != EOF) {
            skipWhitespace();
            if (ch != '(') {
                throw new IOException("Expected command; found: ["+(char)ch+"]");
            }
            ch = in.read();
            skipWhitespace();

            String cmd = readUnquotedToken();
            skipWhitespace();
            if ("build-log".equals(cmd)) {
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

    private void readOp() throws IOException, LensfieldException {
        String taskName = readToken();
        TaskLog task = build.getTask(taskName);

        OperationLog op = new OperationLog(task);

        // Read inputs
        skipWhitespace();
        while (ch != ')') {
            if (ch == EOF) {
                throw new EOFException();
            }
            if (ch == '(') {
                ch = in.read();
                String x = readToken();
                if (!("i".equals(x) || "o".equals(x))) {
                    throw new IOException("Unknown section: "+x);
                }
                skipWhitespace();
                String name = readToken();

                if ("i".equals(x)) {
                    List<String> files = readInput();
                    List<Resource> resources = new ArrayList<Resource>(files.size());
                    for (String path : files) {
                        Resource resource = build.getResource(path);
                        if (resource == null) {
                            throw new LensfieldException("Missing resource record: "+path);
                        }
                        resources.add(resource);
                    }
                    op.addInput(name, resources);
                } else {
                    List<Resource> resources = readOutput();
                    for (Resource resource : resources) {
                        resource.setProducer(op);
                    }
                    op.addOutput(name, resources);
                    build.registerResources(resources);
                }

                skipWhitespace();
            } else {
                throw new IOException();
            }
        }
        ch = in.read();
        skipWhitespace();

        task.addOperation(op);
    }


    private List<String> readInput() throws IOException {
        skipWhitespace();
        List<String> resources = readFileNameList();
        return resources;
    }

    private List<Resource> readOutput() throws IOException {
        skipWhitespace();
        List<Resource> resources = readFileList();
        return resources;
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

    private List<Resource> readFileList() throws IOException {
        List<Resource> files = new ArrayList<Resource>();
        while (ch == '[') {
            ch = in.read();
            files.add(readResource());
            skipWhitespace();
        }
        return files;
    }

    private void readSource() throws IOException, LensfieldException {
        String name = readToken();
        TaskLog task = new TaskLog(name);
        OperationLog op = new OperationLog(task);
        skipWhitespace();
        List<Resource> resources = readFileList();
        for (Resource r : resources) {
            r.setProducer(op);
        }
        if (ch != ')') {
            throw new IOException("Error! ["+((char)ch)+']');
        }
        build.addTask(task);
        task.addOperation(op);
        op.addOutput(name, resources);
        build.registerResources(resources);
    }

    private Resource readResource() throws IOException {
        skipWhitespace();
        String path = readToken();
        skipWhitespace();
        String timestamp = readToken();
        long lastModified = parseDate(timestamp).getMillis();
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

    private void readTask() throws IOException {
        // Read task definition
        String name = readToken();
        skipWhitespace();
        String clazz = readToken();
        LOG.info("Reading task: "+name+" / "+clazz);
        skipWhitespace();
        String timestamp = readToken();
        skipWhitespace();

        TaskLog task = new TaskLog(name);
        task.setClassName(clazz);
        task.setLastModified(parseDate(timestamp).getMillis());

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

    private void readParameters(TaskLog task) throws IOException {
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

    private void readParameter(TaskLog task) throws IOException {
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

    private void readDependencies(TaskLog task) throws IOException {
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

    private void readDependency(TaskLog task) throws IOException {
        skipWhitespace();
        String id = readToken();
        skipWhitespace();
        String timestamp =  readToken();
        skipWhitespace();
        if (ch != ']') {
            throw new IOException("Expected: ']'; found: '"+((char)ch)+"'");
        }
        ch = in.read();
        task.addDependency(new Dependency(id, parseDate(timestamp).getMillis()));
    }

    private void readBuildStarted() throws IOException {
        String version = readToken();
        if (!"version-1.0".equals(version)) {
            throw new IOException("Bad version: "+version);
        }
        skipWhitespace();
        String timestamp = readToken();
        DateTime dateTime = parseDate(timestamp);
        build.setTimeStarted(dateTime);
    }

    private DateTime parseDate(String timestamp) {
        return DateTimeUtils.parseDateTime(timestamp);
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
