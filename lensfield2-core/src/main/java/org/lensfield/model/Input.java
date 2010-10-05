/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.model;

/**
 * @author sea36
 */
public class Input extends Socket {

    private final String step;

    public Input(String name, String step) {
        super(name);
        this.step = step;
    }

    public Input(String step) {
        super();
        this.step = step;
    }

    public String getStep() {
        return step;
    }

}