/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.ops;

import org.apache.commons.io.IOUtils;
import org.lensfield.LensfieldInput;
import org.lensfield.LensfieldOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author sea36
 */
public class Copier {

    @LensfieldInput
    private InputStream in;

    @LensfieldOutput
    private OutputStream out;

    public void run() throws IOException {
        IOUtils.copy(in, out);
    }

}
