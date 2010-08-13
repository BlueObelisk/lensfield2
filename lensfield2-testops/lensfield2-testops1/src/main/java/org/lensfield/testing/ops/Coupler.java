package org.lensfield.testing.ops;

import org.apache.commons.io.IOUtils;
import org.lensfield.api.LensfieldInput;
import org.lensfield.api.LensfieldOutput;
import org.lensfield.api.io.MultiStreamIn;
import org.lensfield.api.io.MultiStreamOut;
import org.lensfield.api.io.StreamIn;
import org.lensfield.api.io.StreamOut;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author sea36
 */
public class Coupler {

    @LensfieldInput
    private MultiStreamIn ins;

    @LensfieldOutput
    private MultiStreamOut outs;


    public void run() throws Exception {

        // Sort inputs
        SortedMap<Integer, StreamIn> inputMap = new TreeMap<Integer, StreamIn>();
        for (StreamIn in = ins.next(); in != null; in = ins.next()) {
            String i = in.getParameter("*");
            inputMap.put(Integer.valueOf(i), in);
        }

        // Generate outputs
        Iterator<Map.Entry<Integer,StreamIn>> it = inputMap.entrySet().iterator();
        Map.Entry<Integer,StreamIn> e0 = it.next();
        while (it.hasNext()) {
            Map.Entry<Integer,StreamIn> e1 = it.next();

            // Open inputs
            StreamIn in0 = e0.getValue();
            StreamIn in1 = e1.getValue();
            in0.reopen();
            in1.reopen();

            // Open output
            String outId = e0.getKey()+"-"+e1.getKey();
            StreamOut out = outs.next();
            out.setParameter("x", outId);

            // Copy inputs to output
            PrintStream ps = new PrintStream(out);
            ps.print(IOUtils.toString(in0));
            ps.print(IOUtils.toString(in1));
            ps.close();

            e0 = e1;
        }

    }    

}
