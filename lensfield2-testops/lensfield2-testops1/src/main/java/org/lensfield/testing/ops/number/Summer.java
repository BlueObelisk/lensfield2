package org.lensfield.testing.ops.number;

import org.apache.commons.io.IOUtils;
import org.lensfield.LensfieldInput;
import org.lensfield.LensfieldOutput;
import org.lensfield.io.MultiStreamIn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author sea36
 */
public class Summer {

    @LensfieldInput
    private MultiStreamIn in;

    @LensfieldOutput
    private OutputStream out;

    public void run() throws IOException {
        int sum = 0;
        for (InputStream is = in.next(); is != null; is = in.next()) {
            String s1;
            try {
                s1 = IOUtils.toString(is);
            } finally {
                is.close();
            }
            sum += Integer.parseInt(s1);
        }
        IOUtils.write(Integer.toString(sum), out);
    }

}