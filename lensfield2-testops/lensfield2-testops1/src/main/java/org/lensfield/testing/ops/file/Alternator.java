package org.lensfield.testing.ops.file;

import org.apache.commons.io.IOUtils;
import org.lensfield.api.LensfieldInput;
import org.lensfield.api.LensfieldOutput;
import org.lensfield.api.io.StreamOut;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * @author sea36
 */
public class Alternator {

    @LensfieldInput
    private InputStream input;

    @LensfieldOutput(name = "odd")
    private OutputStream odd;

    @LensfieldOutput(name = "even")
    private OutputStream even;

    public void run() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        try {
            BufferedWriter out0 = new BufferedWriter(new OutputStreamWriter(odd));
            try {
                BufferedWriter out1 = new BufferedWriter(new OutputStreamWriter(even));
                try {
                    boolean o = true;
                    for (String line = in.readLine(); line != null; line = in.readLine()) {
                        if (o) {
                            out0.write(line);
                            out0.newLine();
                        } else {
                            out1.write(line);
                            out1.newLine();
                        }
                        o = !o;
                    }
                } finally {
                    out1.close();
                }
            } finally {
                out0.close();
            }
        } finally {
            in.close();
        }
    }

}
