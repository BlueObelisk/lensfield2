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

    public static void main(String[] args) throws Exception {

        String lensfieldHome = System.getProperty("lensfield.home");
        if (lensfieldHome == null) {
            System.err.println(" ** ERROR: 'lensfield.home' not set");
            System.exit(1);
        }

        File libdir = new File(lensfieldHome, "lib");
        if (!libdir.isDirectory()) {
            System.err.println(" ** ERROR: directory '"+libdir.getPath()+"' not found");
            System.exit(1);
        }

        List<URL> urls = new ArrayList<URL>();
        for (File f : libdir.listFiles()) {
            try {
                urls.add(f.toURI().toURL());
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
            Class<?> clazz = loader.loadClass("org.lensfield.launcher.load.Loader");
            Method method = clazz.getMethod("main", String[].class);
            method.invoke(null, new Object[]{args});
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

}
