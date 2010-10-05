package org.lensfield.concurrent;

import org.lensfield.model.Input;
import org.lensfield.model.Output;
import org.lensfield.model.Parameter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author sea36
 */
public class TaskRunner {

    private Class<?> clazz;
    private Object object;
    private Method method;
    private boolean staticMethod;

    private Map<Input,Field> inputs;
    private Map<Output, Field> outputs;
    private Map<Parameter,Field> parameters;


    private ClassLoader contextClassLoader;




    private Object invoke(Object[] args) throws Exception {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            Object obj = getObject();
            Object ret = method.invoke(obj, args);
            return ret;
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }
    

    private Object getObject() throws InstantiationException, IllegalAccessException {
        Object obj = createObject();
        configureObject(obj);
        return obj;
    }

    private void configureObject(Object obj) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private Object createObject() throws InstantiationException, IllegalAccessException {
        Object obj;
        if (staticMethod) {
            obj = null;
        } else {
            if (object == null) {
                obj = clazz.newInstance();
            } else {
                obj = object;
            }
        }
        return obj;
    }


}
