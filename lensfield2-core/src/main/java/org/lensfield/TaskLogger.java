/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

/**
 * @author sea36
 */
public class TaskLogger implements Logger {

    private Logger log;
    private String name;


    public TaskLogger(String name, Logger log) {
        this.log = log;
        this.name = name;
    }

    public void debug(String message) {
        log.debug(name, message);
    }

    public void info(String message) {
        log.info(name, message);
    }

    public void warn(String message) {
        log.warn(name, message);
    }

    public void error(String message) {
        log.error(name, message);
    }

    public void debug(String target, String message) {
        log.debug(target, message);
    }

    public void info(String target, String message) {
        log.info(target, message);
    }

    public void warn(String target, String message) {
        log.warn(target, message);
    }

    public void error(String target, String message) {
        log.error(target, message);
    }
    
}
