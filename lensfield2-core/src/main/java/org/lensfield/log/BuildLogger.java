/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.log;

import org.lensfield.build.ParameterDescription;
import org.lensfield.state.BuildState;
import org.lensfield.state.DependencyState;
import org.lensfield.state.FileState;
import org.lensfield.state.TaskState;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class BuildLogger {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

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
        return dateFormat.format(new Date());
    }


    public void startBuild() {
        out.print("(build-started ");
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
        for (TaskState task : buildState.getTasks()) {
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
            out.print(dateFormat.format(new Date(task.getLastModified())));
            if (!task.getParameters().isEmpty()) {
                out.print(" (params ");
                for (ParameterDescription param : task.getParameters()) {
                    if (param.value != null) {
                        writeList(param.getName(), param.getValue());
                    }
                }
                out.print(')');
            }
            if (!task.getDependencyList().isEmpty()) {
                out.print(" (depends ");
                for (DependencyState depend : task.getDependencyList()) {
                    String timestamp = dateFormat.format(new Date(depend.getLastModified()));
                    writeList(depend.getId(), timestamp);
                }
                out.print(')');
            }
            out.println(')');
        }
    }

    public void process(String name, List<FileState> files) {
        out.print("(source ");
        out.print(name);
        out.print("(");
        for (FileState output : files) {
            writeList(output.getPath(), dateFormat.format(output.getLastModified()));
        }
        out.println("))");
    }





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

    public <IF extends FileState, OF extends FileState> void process(String name, Map<String, List<IF>> input, Map<String, List<OF>> output) {
        out.print("(op ");
        out.print(name);
        out.print("(");
        writeInputFiles(input);
        out.print(")(");
        writeOutputFiles(output);
        out.println("))");
    }

    private <FS extends FileState> void writeInputFiles(Map<String, List<FS>> map) {
        for (Iterator<Map.Entry<String,List<FS>>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, List<FS>> e = it.next();
            out.print(e.getKey());
            out.print(' ');
            List<FS> files = e.getValue();
            if (files.size() == 1) {
                FileState f = files.get(0);
                writeToken(f.getPath());
            } else {
                out.print('(');
                for (Iterator<FS> itx = files.iterator(); itx.hasNext();) {
                    FileState f = itx.next();
                    writeToken(f.getPath());
                    if (itx.hasNext()) {
                        out.print(' ');
                    }
                }
                out.print(')');
            }
            if (it.hasNext()) {
                out.print(' ');
            }
        }
    }

    private <FS extends FileState> void writeOutputFiles(Map<String, List<FS>> map) {
        for (Iterator<Map.Entry<String,List<FS>>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, List<FS>> e = it.next();
            out.print(e.getKey());
            out.print(' ');
            List<FS> files = e.getValue();
            if (files.size() == 1) {
                FileState f = files.get(0);
                String[] x = new String[2+2*f.getParams().size()];
                x[0] = f.getPath();
                x[1] = dateFormat.format(f.getLastModified());
                int i = 2;
                for (Map.Entry<String,String> p : f.getParams().entrySet()) {
                    x[i++] = p.getKey();
                    x[i++] = p.getValue();
                }
                writeList(x);
//                writeList(f.getPath(), dateFormat.format(f.getLastModified()));
            } else {
                out.print('(');
                for (FS f : files) {
                    String[] x = new String[2+2*f.getParams().size()];
                    x[0] = f.getPath();
                    x[1] = dateFormat.format(f.getLastModified());
                    int i = 2;
                    for (Map.Entry<String,String> p : f.getParams().entrySet()) {
                        x[i++] = p.getKey();
                        x[i++] = p.getValue();
                    }
                    writeList(x);
                }
                out.print(')');
            }
            if (it.hasNext()) {
                out.print(' ');
            }
        }
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

}


