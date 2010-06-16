/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.testing.ops.file;

import org.apache.commons.io.IOUtils;
import org.lensfield.api.LensfieldInput;
import org.lensfield.api.LensfieldOutput;
import org.lensfield.api.io.StreamOut;
import org.lensfield.api.io.MultiStreamOut;

import java.io.*;

/**
 * @author sea36
 */
public class Splitter {
   
    @LensfieldInput
    private InputStream input;

    @LensfieldOutput
    private MultiStreamOut output;

    public void run() throws IOException {

        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        try {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                StreamOut out = output.next();
                try {
                    IOUtils.write(line, out);
                } finally {
                    out.close();
                }
            }
        } finally {
            in.close();
        }        

    }

}
