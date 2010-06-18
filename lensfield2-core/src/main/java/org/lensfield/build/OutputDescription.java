/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.build;

import org.lensfield.api.io.MultiStreamOut;
import org.lensfield.glob.Template;
import org.lensfield.state.TaskState;

import java.io.OutputStream;
import java.lang.reflect.Field;

/**
* @author sea36
*/
public class OutputDescription {

    private final TaskState task;
    private final String name;

    private boolean arg;
    private Field field;
    private boolean multifile;
    private Template glob;

    public OutputDescription(TaskState task, String name) {
        this.task = task;
        this.name = name;
    }

    public OutputDescription(TaskState task, Class<?> clazz) {
        this.task = task;
        this.arg = false;
        this.name = "out";
        this.field = null;
        this.multifile = false;
    }

    public OutputDescription(TaskState task, Field field, String name) {
        this.task = task;
        this.arg = false;
        this.field = field;
        this.name = name;
        Class<?> clazz = field.getType();
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

    public boolean isArg() {
        return arg;
    }

    public String getName() {
        return name;
    }

    public Field getField() {
        return field;
    }

    public boolean isMultifile() {
        return multifile;
    }

    public Template getGlob() {
        return glob;
    }

    public void setGlob(Template glob) {
        this.glob = glob;
    }
}
