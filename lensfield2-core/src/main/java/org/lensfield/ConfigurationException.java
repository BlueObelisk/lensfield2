/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

/**
 * @author sea36
 */
public class ConfigurationException extends LensfieldException {

    public ConfigurationException() {
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }

}
