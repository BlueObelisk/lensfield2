/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.launcher.boot;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sea36
 */
public class Bootstrap {

    private static final boolean DEBUG = Boolean.getBoolean("lensfield.debug");

    public static void main(String[] args) throws Exception {

        if (DEBUG) {
            System.err.println("*** Starting Bootstrap ***");
        }

        String lensfieldHome = System.getProperty("lensfield.home");
        if (lensfieldHome == null) {
            System.err.println(" ** ERROR: 'lensfield.home' not set");
            System.exit(1);
        }
        if (DEBUG) {
            System.err.println("lensfield.home : "+lensfieldHome);
        }

        File libdir = new File(lensfieldHome, "lib");
        if (!libdir.isDirectory()) {
            System.err.println(" ** ERROR: directory '"+libdir.getPath()+"' not found");
            System.exit(1);
        }
        if (DEBUG) {
            System.err.println("lib dir: "+lensfieldHome);
        }

        List<URL> urls = new ArrayList<URL>();
        for (File f : libdir.listFiles()) {
            try {
                URL url = f.toURI().toURL();
                urls.add(url);
                if (DEBUG) {
                    System.err.println("url : "+url);
                }
            } catch (MalformedURLException e) {
                // Impossible!
                throw new Error("Malformed URL from java.io.File", e);
            }
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            // Init class loader
            URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
            Thread.currentThread().setContextClassLoader(loader);

            // Launch app loader
            Method method;
            try {
                Class<?> clazz = loader.loadClass("org.lensfield.launcher.load.Loader");
                method = clazz.getMethod("main", String[].class);
            } catch (Exception e) {
                System.err.println(" ** ERROR launching lensfield");
                e.printStackTrace();
                throw e;
            }
            if (DEBUG) {
                System.err.println("launching lensfield loader...");
            }
            method.invoke(null, new Object[]{args});
            if (DEBUG) {
                System.err.println("lensfield finished...");
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

}
