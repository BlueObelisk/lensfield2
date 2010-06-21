/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.build;

import java.lang.reflect.Field;

/**
* @author sea36
*/
public class ParameterDescription {

    private final String name;
    private String value;

    private String fieldName;
    private String fieldClass;

    private transient final boolean required;

    public ParameterDescription(Field f, String name, String value, boolean required) {
        this.name = name;
        this.fieldName = f.getName();
        this.fieldClass = f.getDeclaringClass().getName();
        this.value = value;
        this.required = required;
    }

    public ParameterDescription(String name, String value) {
        this.name = name;
        this.value = value;
        required = false;
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

    public String getValue() {
        return value;
    }

    public boolean isRequired() {
        return required;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
