/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

/**
 * @author sea36
 */
public class LensfieldException extends Exception {

    public LensfieldException() {
    }

    public LensfieldException(String message) {
        super(message);
    }

    public LensfieldException(String message, Throwable cause) {
        super(message, cause);
    }

    public LensfieldException(Throwable cause) {
        super(cause);
    }
}
