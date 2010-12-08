/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import org.lensfield.api.io.MultiStreamIn;
import org.lensfield.concurrent.ParameterSet;
import org.lensfield.concurrent.Resource;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
* @author sea36
*/
public class InputPipe {

    private final Process process;
    private final String name;

    private String fieldName;
    private String fieldClass;

    private OutputPipe source;

    private final boolean multifile;

    public InputPipe(Process process) {
        this.process = process;
        this.name = "in";
        this.multifile = false;
    }

    public InputPipe(Process process, Field f, String name) {
        this.process = process;
        this.name = name;
        this.fieldName = f.getName();
        this.fieldClass = f.getDeclaringClass().getName();
        Class<?> clazz = f.getType();
        this.multifile = getType(clazz);
    }

    private boolean getType(Class<?> clazz) {
        if (InputStream.class.isAssignableFrom(clazz)) {
            return false;
        } else if (MultiStreamIn.class.isAssignableFrom(clazz)) {
            return true;
        } else {
            throw new RuntimeException("Unknown type: "+clazz.getName());
        }
    }


    public Process getProcess() {
        return process;
    }

    public String getName() {
        return name;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldClass() {
        return fieldClass;
    }

    public boolean isMultifile() {
        return multifile;
    }


    public void add(Resource resource) {
        Map<String,String> globMatch = new LinkedHashMap<String,String>();
        for (String name : process.getCommonGlobNames()) {
            globMatch.put(name, resource.getParameters().get(name));
        }

        OperationKey operationKey = new OperationKey(process, new ParameterSet(globMatch));
        Operation op = process.getOperation(operationKey);

//        Resource oldResource = op.getResource(this);
//        if (oldResource != null && isOutOfDate(resource, oldResource)) {
//            op.setOutOfDate(true);
//        }

        op.addInputResource(this, resource);
        if (op.isReady()) {
            // TODO addResource to queue
            process.queue(op);
        }
    }


    public OutputPipe getSource() {
        return source;
    }

    public void setSource(OutputPipe source) {
        this.source = source;
    }

    public boolean isClosed() {
        return source.isClosed();
    }

    public void close() {
        process.check();
    }

}
