/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.build;

import org.lensfield.api.io.MultiStreamOut;
import org.lensfield.glob.Glob;
import org.lensfield.state.TaskState;

import java.io.OutputStream;
import java.lang.reflect.Field;

/**
* @author sea36
*/
public class OutputDescription {

    private final TaskState task;
    private final String name;

    private String fieldName;
    private String fieldClass;

    private boolean multifile;
    private Glob glob;

    public OutputDescription(TaskState task, String name) {
        this.task = task;
        this.name = name;
    }

    public OutputDescription(TaskState task, Class<?> clazz) {
        this.task = task;
        this.name = "out";
        this.multifile = false;
    }

    public OutputDescription(TaskState task, Field f, String name) {
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


    public TaskState getTask() {
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
}
