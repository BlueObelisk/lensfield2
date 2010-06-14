/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.model;

/**
 * @author sea36
 */
public class Parameter {

    private final String name;
    private final String value;

    public Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Parameter(String value) {
        this.name = null;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
    
}
