/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.glob;

import org.lensfield.LensfieldException;

/**
 * @author sea36
 */
public class MissingParameterException extends LensfieldException {

    private String name;

    public MissingParameterException(String name) {
        super("Missing parameter: "+name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
}
