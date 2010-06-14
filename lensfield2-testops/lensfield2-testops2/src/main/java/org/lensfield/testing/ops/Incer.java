package org.lensfield.testing.ops;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author sea36
 */
public class Incer {

    public void run(InputStream in, OutputStream out) throws IOException {
        String s1 = IOUtils.toString(in);
        String s2 = Integer.toString(1+Integer.valueOf(s1));
        IOUtils.write(s2, out);
    }

}