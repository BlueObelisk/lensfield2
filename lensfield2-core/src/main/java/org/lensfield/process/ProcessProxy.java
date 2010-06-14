/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.process;

import org.lensfield.LensfieldException;
import org.lensfield.LensfieldProcess;

import java.lang.reflect.Method;

/**
 * @author sea36
 */
public class ProcessProxy implements LensfieldProcess {

    private Object obj;
    private Method run;

    public ProcessProxy(Object obj, Method run) {
        this.obj = obj;
        this.run = run;
    }

    public ProcessProxy(Object obj) throws LensfieldException {
        Class<?> clz = obj.getClass();
        this.obj = obj;
        try {
            this.run = clz.getMethod("run");
        } catch (NoSuchMethodException e) {
            throw new LensfieldException("Process must implement no-args run method");
        }
    }

    public void run() throws Exception {
        run.invoke(obj);        
    }
    
}
