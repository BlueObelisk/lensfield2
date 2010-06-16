/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.api.Logger;

/**
 * @author sea36
 */
public class LoggerImpl implements Logger {

    public void debug(String message) {
        System.err.print("[DEBUG] ");
        System.err.println(message);
    }

    public void info(String message) {
        System.err.print("[INFO]  ");
        System.err.println(message);
    }

    public void warn(String message) {
        System.err.print("[WARN]  ");
        System.err.println(message);
    }

    public void error(String message) {
        System.err.print("[ERROR] ");
        System.err.println(message);
    }


    public void debug(String target, String message) {
        System.err.print("[DEBUG] ("+target+") ");
        System.err.println(message);
    }

    public void info(String target, String message) {
        System.err.print("[INFO]  ("+target+") ");
        System.err.println(message);
    }

    public void warn(String target, String message) {
        System.err.print("[WARN]  ("+target+") ");
        System.err.println(message);
    }

    public void error(String target, String message) {
        System.err.print("[ERROR] ("+target+") ");
        System.err.println(message);
    }

}
