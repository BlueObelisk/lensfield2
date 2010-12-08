/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import org.lensfield.api.io.MultiStreamOut;
import org.lensfield.concurrent.Resource;
import org.lensfield.glob.Glob;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sea36
 */
public class OutputPipe {

    private final Process task;
    private final String name;

    private volatile boolean closed;

    private String fieldName;
    private String fieldClass;

    private boolean multifile;
    private Glob glob;

    private List<InputPipe> pipes = new ArrayList<InputPipe>();

    public OutputPipe(Process task, String name) {
        this.task = task;
        this.name = name;
    }

    public OutputPipe(Process task) {
        this.task = task;
        this.name = "out";
        this.multifile = false;
    }

    public OutputPipe(Process task, Field f, String name) {
        this.task = task;
        this.fieldName = f.getName();
        this.fieldClass = f.getDeclaringClass().getName();
        this.name = name;
        Class<?> clazz = f.getType();
        this.multifile = getType(clazz);
    }

    private boolean getType(Class<?> clazz) {
        if (OutputStream.class.isAssignableFrom(clazz)) {
            return false;
        } else if (MultiStreamOut.class.isAssignableFrom(clazz)) {
            return true;
        } else {
            throw new RuntimeException("Unknown type: "+clazz.getName());
        }
    }


    public Process getTask() {
        return task;
    }

    public String getName() {
        return name;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldClass() {
        return fieldClass;
    }

    public boolean isMultifile() {
        return multifile;
    }

    public Glob getGlob() {
        return glob;
    }

    public void setGlob(Glob glob) {
        this.glob = glob;
    }

    public void addPipe(InputPipe input) {
        input.setSource(this);
        pipes.add(input);
    }

    public void sendResource(Resource resource) {
        for (InputPipe input : pipes) {
            input.add(resource);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        this.closed = true;
        for (InputPipe pipe : pipes) {
            pipe.close();
        }
    }

}
