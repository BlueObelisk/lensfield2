package org.lensfield.testing.ops.number;

import org.apache.commons.io.IOUtils;
import org.lensfield.api.LensfieldInput;
import org.lensfield.api.LensfieldOutput;
import org.lensfield.api.LensfieldParameter;
import org.lensfield.api.io.StreamIn;
import org.lensfield.api.io.StreamOut;

import java.io.IOException;

/**
 * @author sea36
 */
public class Multiplier {

    @LensfieldInput
    private StreamIn in;

    @LensfieldOutput
    private StreamOut out;

    @LensfieldParameter
    private String factor1;

    @LensfieldParameter
    private String factor2;

    public void run() throws IOException {
        int f1 = Integer.parseInt(factor1);
        int f2 = Integer.parseInt(factor2);
        String s1 = IOUtils.toString(in);
        Integer i = Integer.parseInt(s1);
        String s2 = Integer.toString(i * f1 * f2);
        IOUtils.write(s2, out);
    }

}