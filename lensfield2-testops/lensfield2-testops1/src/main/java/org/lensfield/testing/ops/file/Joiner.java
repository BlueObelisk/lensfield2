/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.testing.ops.file;

import org.lensfield.api.LensfieldInput;
import org.lensfield.api.LensfieldOutput;
import org.apache.commons.io.IOUtils;
import org.lensfield.api.io.MultiStreamIn;
import org.lensfield.api.io.StreamOut;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author sea36
 */
public class Joiner {

    @LensfieldInput
    private MultiStreamIn in;

    @LensfieldOutput
    private StreamOut out;

    public void run() throws IOException {
        try {
            for (InputStream is = in.next(); is != null; is = in.next()) {
                IOUtils.copy(is,out);
                is.close();
            }
        } finally {
            out.close();
        }        
    }

}
