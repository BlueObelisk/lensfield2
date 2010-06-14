/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.model;

/**
 * @author sea36
 */
public class Input {

    private final String name;
    private final String step;

    public Input(String name, String step) {
        this.name = name;
        this.step = step;
    }

    public Input(String step) {
        this.name = null;
        this.step = step;
    }

    public String getName() {
        return name;
    }

    public String getStep() {
        return step;
    }

}