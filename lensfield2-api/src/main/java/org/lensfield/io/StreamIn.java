/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author sea36
 */
public abstract class StreamIn extends InputStream {

    public abstract void reopen() throws IOException;

    public abstract String getParameter(String name);

}