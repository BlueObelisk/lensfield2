/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.testing.ops.file;

import org.apache.commons.io.IOUtils;
import org.lensfield.api.LensfieldInput;
import org.lensfield.api.LensfieldOutput;

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
