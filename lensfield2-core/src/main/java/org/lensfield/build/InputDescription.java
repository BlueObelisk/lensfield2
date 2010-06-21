/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.build;

import org.lensfield.api.io.MultiStreamIn;
import org.lensfield.state.TaskState;

import java.io.InputStream;
import java.lang.reflect.Field;

/**
* @author sea36
*/
public class InputDescription {

    private TaskState task;
    private final String name;

    private String fieldName;
    private String fieldClass;

    private final boolean multifile;

    public InputDescription(Class<?> clazz) {
        this.name = "in";
        this.multifile = false;
    }

    public InputDescription(Field f, String name) {
        this.name = name;
        this.fieldName = f.getName();
        this.fieldClass = f.getDeclaringClass().getName();
        Class<?> clazz = f.getType();
        this.multifile = getType(clazz);
    }

    private boolean getType(Class<?> clazz) {
        if (InputStream.class.isAssignableFrom(clazz)) {
            return false;
        } else if (MultiStreamIn.class.isAssignableFrom(clazz)) {
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
}
