package org.lensfield.test.logging;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author sea36
 */
public class LogOp {

    private static final Logger log = Logger.getLogger(LogOp.class);

    static {
        System.err.println("--------------------");
        System.err.println("class: "+LogOp.class);
        System.err.println("class-loader: "+LogOp.class.getClassLoader());
        System.err.println("class-parent: "+LogOp.class.getClassLoader().getParent());
        System.err.println("logger: "+log.getClass());
        System.err.println("log-loader: "+log.getClass().getClassLoader());
        System.err.println("log-parent: "+log.getClass().getClassLoader().getParent());
        System.err.println("====================");
    }

    public void run(InputStream in, OutputStream out) {
        log.info("running...");
    }

}
