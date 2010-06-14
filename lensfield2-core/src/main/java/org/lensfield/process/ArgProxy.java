/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.process;

import org.lensfield.LensfieldProcess;
import org.lensfield.io.StreamIn;
import org.lensfield.io.StreamOut;

import java.lang.reflect.Method;

/**
 * @author sea36
 */
public class ArgProxy implements LensfieldProcess {

    private Object obj;
    private Method runMethod;
    private StreamIn in;
    private StreamOut out;

    public ArgProxy(Object obj, Method runMethod, StreamIn input, StreamOut output) {
        this.obj = obj;
        this.runMethod = runMethod;
        this.in = input;
        this.out = output;
    }

    public void run() throws Exception {
        runMethod.invoke(obj, in, out);
    }
    
}
