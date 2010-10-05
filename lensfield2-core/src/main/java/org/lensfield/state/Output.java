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
public class Output {

    private final Process task;
    private final String name;

    private volatile boolean closed;

    private String fieldName;
    private String fieldClass;

    private boolean multifile;
    private Glob glob;

    private List<Input> pipes = new ArrayList<Input>();

    public Output(Process task, String name) {
        this.task = task;
        this.name = name;
    }

    public Output(Process task) {
        this.task = task;
        this.name = "out";
        this.multifile = false;
    }

    public Output(Process task, Field f, String name) {
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

    public void addPipe(Input input) {
        input.setSource(this);
        pipes.add(input);
    }

    public void addResource(Resource resource) {
        String id = task.getId()+"/"+name;
        System.err.println(id+" == "+resource.getPath());
        for (Input input : pipes) {
            System.err.println(id+" >> "+input.getProcess().getId()+"/"+input.getName());
            input.add(resource);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        this.closed = true;
        for (Input pipe : pipes) {
            pipe.close();
        }
    }

}
