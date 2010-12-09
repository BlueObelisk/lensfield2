/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.log;

import org.joda.time.DateTime;
import org.lensfield.concurrent.Resource;
import org.lensfield.concurrent.ResourceSet;
import org.lensfield.state.*;
import org.lensfield.state.Process;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class BuildLogger {

    private PrintWriter out;

    public BuildLogger(OutputStream out) {
        Writer w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported!", e);
        }
        this.out = new PrintWriter(w, true);
    }

    private String now() {
        DateTime now = new DateTime();
        return DateTimeUtils.formatDateTime(now);
    }


    public void startBuild() {
        out.print("(build-log version-1.0 ");
        out.print(now());
        out.println(')');
    }


    public void finishBuild() {
        out.print("(build-finished ");
        out.print(now());
        out.println(')');
    }


    public void close() {
        if (out != null) {
            out.close();
            out = null;
        }
    }


    public void recordTasks(BuildState buildState) {
        for (org.lensfield.state.Process task : buildState.getTasks()) {
            if (task.getInputs().isEmpty()) {
                // source
                continue;
            }
            out.print("(task ");
            out.print(task.getId());
            out.print(' ');
            out.print(task.getClassName());
            out.print('/');
            out.print(task.getMethodName());
            out.print(' ');
            out.print(DateTimeUtils.formatDateTime(new DateTime(task.getLastModified())));
            if (!task.getParameters().isEmpty()) {
                out.print(" (params ");
                for (Parameter param : task.getParameters()) {
                    if (param.getValue() != null) {
                        writeList(param.getName(), param.getValue());
                    }
                }
                out.print(')');
            }
            if (!task.getDependencyList().isEmpty()) {
                out.print(" (depends ");
                for (Dependency depend : task.getDependencyList()) {
                    String timestamp = DateTimeUtils.formatDateTime(new DateTime(depend.getLastModified()));
                    writeList(depend.getId(), timestamp);
                }
                out.print(')');
            }
            out.println(')');
        }
    }

//    public void process(String name, List<FileState> files) {
//        out.print("(source ");
//        out.print(name);
//        out.print("(");
//        for (FileState output : files) {
//            writeList(output.getPath(), DATE_FORMAT.format(output.getLastModified()));
//        }
//        out.println("))");
//    }





    private void writeToken(String s) {
        if (s.length() == 0) {
            out.print("''");
        }
        else if (needsQuoting(s)) {
            writeQuotedToken(s);
        } else {
            writeUnquotedToken(s);
        }
    }

    private void writeUnquotedToken(String s) {
        out.print(s);
    }

    private void writeQuotedToken(String s) {
        if (s.indexOf('"') == -1) {
            writeQuotedToken('"', s);
        }
        else if (s.indexOf('\'') == -1) {
            writeQuotedToken('\'', s);
        }
        else {
            writeQuotedToken('"', s.replace("\"", "\\\""));
        }
    }

    private void writeQuotedToken(char c, String s) {
        out.print(c);
        out.print(s);
        out.print(c);
    }

    private boolean needsQuoting(String s) {
        if (containsWhitespace(s)) {
            return true;
        }
        return containsSpecialCharacter(s);
    }

    private boolean containsSpecialCharacter(String s) {
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\'' || c == '(' || c == ')' || c == '[' || c == ']') {
                return true;
            }
        }
        return false;
    }

    private boolean containsWhitespace(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
    }


    private void writeList(String... list) {
        out.print('[');
        for (int i = 0; i < list.length; i++) {
            if (i != 0) {
                out.print(' ');
            }
            writeToken(list[i]);
        }
        out.print(']');
    }

    public void logSource(Process task, List<Resource> resources) {
        out.print("(source ");
        out.print(task.getId());
        out.print(' ');
        writeResources(resources);
        out.println(')');
    }

    public synchronized void logOperation(Operation operation, Map<OutputPipe, List<Resource>> outputResourcesMap) {

        out.print("(op ");
        out.print(operation.getProcess().getId());
        for (Map.Entry<InputPipe, ResourceSet> e : operation.getInputResourcesMap().entrySet()) {
            InputPipe input = e.getKey();

            out.print(" (i ");
            out.print(input.getName());

            List<Resource> resources = e.getValue().getResourceList();
            for (Resource r : resources) {
                out.print(' ');
                writeToken(r.getPath());
            }

            out.print(')');
        }
        for (OutputPipe output : operation.getOutputSet()) {
            out.print(" (o ");
            out.print(output.getName());
            out.print(' ');

            List<Resource> resources = outputResourcesMap.get(output);
            writeResources(resources);
            out.print(')');
        }
        out.println(')');
    }

    private void writeResources(List<Resource> resources) {
        for (Resource r : resources) {
            writeList(r.getPath(), DateTimeUtils.formatDateTime(new DateTime(r.getLastModified())));
        }
    }

}


