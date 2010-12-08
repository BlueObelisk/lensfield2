package org.lensfield.concurrent;

import org.lensfield.LensfieldException;

/**
 * @author Sam Adams
 */
public class DuplicateResourceException extends LensfieldException {

    public DuplicateResourceException(String message) {
        super(message);
    }
    
}
