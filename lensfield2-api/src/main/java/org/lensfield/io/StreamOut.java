/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author sea36
 */
public abstract class StreamOut extends OutputStream {

    public abstract void reopen() throws IOException;

    public abstract void setParameter(String name, String value);
    
}
