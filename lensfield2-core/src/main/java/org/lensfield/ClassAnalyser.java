/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.api.LensfieldInput;
import org.lensfield.api.LensfieldOutput;
import org.lensfield.api.LensfieldParameter;
import org.lensfield.build.InputDescription;
import org.lensfield.build.OutputDescription;
import org.lensfield.build.ParameterDescription;
import org.lensfield.model.Build;
import org.lensfield.model.Parameter;
import org.lensfield.state.TaskState;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sea36
 */
public class ClassAnalyser {

    public static void analyseClass(Build build, TaskState task) throws Exception {
        checkNoArgConstructor(task);
        findRunMethod(task);
        analyseFields(task);
        setParameterValues(build, task);
    }


    /**
     * Throws exception if class instance cannot be created
     */
    private static void checkNoArgConstructor(TaskState task) throws IllegalAccessException, InstantiationException {
        Class<?> clazz = task.getClazz();
        clazz.newInstance();
    }


    private static void findRunMethod(TaskState task) {
        findRunMethod(task,task.getClazz());
    }

    private static boolean findRunMethod(TaskState task, Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (task.getMethodName().equals(method.getName())) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0) {
                    task.setMethod(method, true);
                    return true;
                }
                if (parameterTypes.length == 2) {
                    if ( InputStream.class.isAssignableFrom(parameterTypes[0])
                            && OutputStream.class.isAssignableFrom(parameterTypes[1])) {
                        task.setMethod(method, false);
                        task.addInput(new InputDescription(parameterTypes[0]));
                        task.addOutput(new OutputDescription(parameterTypes[1]));
                        return true;
                    }
                }
            }
        }
        Class<?> parent = clazz.getSuperclass();
        if (parent != null) {
            return findRunMethod(task, parent);
        }
        return false;
    }



    private static void analyseFields(TaskState task) throws LensfieldException {
        analyseClass(task, task.getClazz());
    }

    private static void analyseClass(TaskState task, Class<?> clazz) throws LensfieldException {
        for (Field f : clazz.getDeclaredFields()) {
            if (isInput(f)) {
                addInput(task, f);
            }
            else if (isOutput(f)) {
                addOutput(task, f);
            }
            else if (isParameter(f)) {
                addParameter(task, f);
            }
        }
        Class<?> parent = clazz.getSuperclass();
        if (parent != null) {
            analyseClass(task, parent);
        }
    }


    private static boolean isInput(Field f) {
        return f.isAnnotationPresent(LensfieldInput.class);
    }

    private static boolean isOutput(Field f) {
        return f.isAnnotationPresent(LensfieldOutput.class);
    }

    private static boolean isParameter(Field f) {
        return f.isAnnotationPresent(LensfieldParameter.class);
    }


    private static void addInput(TaskState task, Field f) throws LensfieldException {
        if (!task.isNoArgs()) {
            throw new LensfieldException("@LensfieldInput fields require no-args run method");
        }
        LensfieldInput annot = f.getAnnotation(LensfieldInput.class);
        String n = "".equals(annot.name()) ? f.getName() : annot.name();
        task.addInput(new InputDescription(f, n));
    }

    private static void addOutput(TaskState task, Field f) throws LensfieldException {
        if (!task.isNoArgs()) {
            throw new LensfieldException("@LensfieldOutput fields require no-args run method");
        }
        LensfieldOutput annot = f.getAnnotation(LensfieldOutput.class);
        String n = "".equals(annot.name()) ? f.getName() : annot.name();
        task.addOutput(new OutputDescription(f, n));
    }

    private static void addParameter(TaskState task, Field f) {
        LensfieldParameter annot = f.getAnnotation(LensfieldParameter.class);
        String n = "".equals(annot.name()) ? f.getName() : annot.name();
        task.addParameter(new ParameterDescription(f, n, null, !annot.optional()));
    }



    private static void setParameterValues(Build build, TaskState task) throws LensfieldException {
        if (task.getParameters().size() == 1) {
            if (build.getParameters().size() == 1) {
                Parameter p = build.getParameters().get(0);
                if (p.getName() == null) {
                    build.getParameters().clear();
                    build.getParameters().add(new Parameter(task.getParameters().get(0).name, p.getValue()));
                }
            }
        }

        Map<String,String> paramMap = new HashMap<String, String>();
        for (Parameter param : build.getParameters()) {
            // TODO check for duplicates
            paramMap.put(param.getName(), param.getValue());
        }
        for (ParameterDescription param : task.getParameters()) {
            if (paramMap.containsKey(param.name)) {
                param.value = paramMap.get(param.name);
            } else {
                if (param.required) {
                    throw new LensfieldException("Required parameter missing: "+param.name);
                }
            }
        }
    }


}
