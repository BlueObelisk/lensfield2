/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.launcher.load;

import org.lensfield.maven.DependencyResolver;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.ArtifactResolutionException;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sea36
 */
public class Loader {

    private static final boolean DEBUG = Boolean.getBoolean("lensfield.debug");

    private URLClassLoader apiLoader;
    private URLClassLoader appLoader;

    private void load(boolean update) throws Exception {

        DependencyResolver resolver = new DependencyResolver();
        resolver.setUseMavenCentral(false);

        if (update) {
            if (DEBUG) {
                System.err.println("adding remote repository");
            }
            resolver.addRepository("ucc-repo", "https://maven.ch.cam.ac.uk/m2repo");
        }

        // Add local lensfield repo
        String lensfieldHome = System.getProperty("lensfield.home");
        File repoDir = new File(lensfieldHome, "repo").getAbsoluteFile();
        URL repoUrl = repoDir.toURI().toURL();
        resolver.addRepository("lf-local", repoUrl.toString());
        if (DEBUG) {
            System.err.println("local-repo: "+repoUrl);
        }

        List<URL> apiUrls = new ArrayList<URL>();
        for (Artifact a : resolver.resolveDependencies("org.lensfield", "lensfield2-api", "0.2-SNAPSHOT")) {
            URL url = a.getFile().toURI().toURL();
            apiUrls.add(url);
            if (DEBUG) {
                System.err.println("api url: "+url);
            }
        }
        apiLoader = new URLClassLoader(apiUrls.toArray(new URL[apiUrls.size()]));

        List<URL> appUrls = new ArrayList<URL>();
        for (Artifact a : resolver.resolveDependencies("org.lensfield", "lensfield2-cli", "0.2-SNAPSHOT")) {
            URL url = a.getFile().toURI().toURL();
            appUrls.add(url);
            if (DEBUG) {
                System.err.println("app url: "+url);
            }
        }
        appLoader = new URLClassLoader(appUrls.toArray(new URL[appUrls.size()]), apiLoader);
    }

    private void run(String[] args) throws Exception {

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Set context class loader
            Thread.currentThread().setContextClassLoader(appLoader);

            // Launch app loader
            Class<?> clazz = appLoader.loadClass("org.lensfield.cli.LensfieldCli");
            Method method = clazz.getMethod("main", String[].class);
            method.invoke(null, new Object[]{args});
        } finally {
            // Restore context class loader
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

    }

    public static void main(String[] args) throws Exception {

        if (DEBUG) {
            System.err.println("*** Starting Loader ***");
        }

        boolean update;
        if (args.length == 1 && "--update".equals(args[0])) {
            System.err.println("Updating lensfield...");
            update = true;
        } else {
            update = false;
        }

        if (DEBUG) {
            System.err.println("update: "+update);
        }

        Loader loader = new Loader();
        try {
            loader.load(update);
        } catch (ArtifactResolutionException e) {
            throw e;
        }
        if (update) {
            System.err.println("Update complete");
            System.exit(0);
        }
        try {
            if (DEBUG) {
                System.err.println("Launching lensfield CLI...");
            }
            loader.run(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println();
            System.err.println(" ** BUILD FAILED **");
            System.exit(1);
        }

    }


}
