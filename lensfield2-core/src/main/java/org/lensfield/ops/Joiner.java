/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.ops;

import org.lensfield.LensfieldInput;
import org.lensfield.LensfieldOutput;
import org.apache.commons.io.IOUtils;
import org.lensfield.io.MultiStreamIn;
import org.lensfield.io.StreamOut;

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
