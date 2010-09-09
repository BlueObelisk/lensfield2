/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.model;

/**
 * @author sea36
 */
public class Output extends Resource {

    private final String value;

    public Output(String name, String value) {
        super(name);
        this.value = value;
    }

    public Output(String value) {
        super();
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}