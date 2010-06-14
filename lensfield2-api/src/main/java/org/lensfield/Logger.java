/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

/**
 * @author sea36
 */
public interface Logger {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message);


    void debug(String target, String message);

    void info(String target, String message);

    void warn(String target, String message);

    void error(String target, String message);

}
