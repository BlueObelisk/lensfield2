/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author sea36
 */
public abstract class MultiStreamIn {

    public abstract StreamIn next() throws IOException;
    
}
