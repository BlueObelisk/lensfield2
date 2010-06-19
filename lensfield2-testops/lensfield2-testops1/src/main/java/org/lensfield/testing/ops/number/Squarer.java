/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.testing.ops.number;

import org.apache.commons.io.IOUtils;
import org.lensfield.api.LensfieldInput;
import org.lensfield.api.LensfieldOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author sea36
 */
public class Squarer {

    @LensfieldInput
    private InputStream input;

    @LensfieldOutput
    private OutputStream output;

    public void run() throws IOException {
        String s1 = IOUtils.toString(input);
        Integer i = Integer.parseInt(s1);
        String s2 = Integer.toString(i * i);
        IOUtils.write(s2, output);
    }

}