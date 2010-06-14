package org.lensfield.testing.ops.number;

import org.apache.commons.io.IOUtils;
import org.lensfield.LensfieldInput;
import org.lensfield.LensfieldOutput;
import org.lensfield.io.StreamIn;
import org.lensfield.io.StreamOut;

import java.io.IOException;

/**
 * @author sea36
 */
public class Doubler {

    @LensfieldInput
    private StreamIn in;

    @LensfieldOutput
    private StreamOut out;

    public void run() throws IOException {
        String s1 = IOUtils.toString(in);
        Integer i = Integer.parseInt(s1);
        String s2 = Integer.toString(i * 2);
        IOUtils.write(s2, out);
    }

}
