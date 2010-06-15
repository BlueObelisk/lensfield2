/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.build;

import org.lensfield.io.MultiStreamIn;

import java.io.InputStream;
import java.lang.reflect.Field;

/**
* @author sea36
*/
public class InputDescription {

    public final boolean arg;
    public final String name;
    public final Field field;
    public final boolean multifile;

    public InputDescription(Class<?> clazz) {
        this.arg = false;
        this.name = "in";
        this.field = null;
        this.multifile = false;
    }

    public InputDescription(Field field, String name) {
        this.arg = false;
        this.field = field;
        this.name = name;
        Class<?> clazz = field.getType();
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

}
