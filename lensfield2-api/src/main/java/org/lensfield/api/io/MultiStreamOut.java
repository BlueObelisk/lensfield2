/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.api.io;

import java.io.IOException;

/**
 * @author sea36
 */
public abstract class MultiStreamOut {

    public abstract StreamOut next() throws IOException;

}