/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.model;

/**
 * @author sea36
 */
public class Parameter extends Resource {

    private final String value;

    public Parameter(String name, String value) {
        super(name);
        this.value = value;
    }

    public Parameter(String value) {
        super();
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
}
