/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.build;

import java.lang.reflect.Field;

/**
* @author sea36
*/
public class ParameterDescription {

    public final String name;
    public String value;

    public transient final Field field;
    public transient final boolean required;

    public ParameterDescription(Field f, String name, String value, boolean required) {
        this.name = name;
        this.field = f;
        this.value = value;
        this.required = required;
    }

    public ParameterDescription(String name, String value) {
        this.name = name;
        this.value = value;
        field = null;
        required = false;
    }


    public String getName() {
        return name;
    }

    public Field getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    public boolean isRequired() {
        return required;
    }
}
