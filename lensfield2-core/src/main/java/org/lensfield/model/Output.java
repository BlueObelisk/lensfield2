/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.model;

/**
 * @author sea36
 */
public class Output {

    private final String name;
    private final String value;

    public Output(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Output(String value) {
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