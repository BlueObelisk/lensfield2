/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author sea36
 */
public abstract class MultiStreamOut {

    public abstract StreamOut next() throws IOException;

}