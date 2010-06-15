/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.build;

import org.lensfield.io.MultiStreamOut;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Field;

/**
* @author sea36
*/
public class OutputDescription {

    public final boolean arg;
    public final String name;
    public final Field field;
    public final boolean multifile;

    public OutputDescription(Class<?> clazz) {
        this.arg = false;
        this.name = "out";
        this.field = null;
        this.multifile = false;
    }

    public OutputDescription(Field field, String name) {
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
}
