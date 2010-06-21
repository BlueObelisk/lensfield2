/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.cli;

import org.lensfield.BuildFileReader;
import org.lensfield.Lensfield;
import org.lensfield.model.Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author sea36
 */
public class LensfieldCli {

    private static String loadVersion() throws IOException {
        InputStream in = LensfieldCli.class.getResourceAsStream("/META-INF/lensfield.version");
        if (in == null) {
            return "-undefined-";
        }
        try {
            String version = new BufferedReader(new InputStreamReader(in)).readLine();
            return version;
        } finally {
            in.close();
        }
    }


    public static void main(String[] args) throws Exception {

        String version = loadVersion();

        System.err.println("Lensfield2 ("+version+")");
        System.err.println("----------------------------------------");

        // Locate build file
        File buildFile = new File("build.lf");
        if (args.length > 0) {
            File file = new File(args[0]);
            if (file.isFile()) {
                buildFile = file;
            }
            else if (file.isDirectory()) {
                buildFile = new File(file, "build.lf");
            }
        }

        if (!buildFile.isFile()) {
            System.err.println(" ** ERROR ** 'build.lf' not found");
            System.err.println("Usage:");
            System.err.println("  lf [build path]");
            System.exit(1);
        }

        // Parse build file
        BuildFileReader parser = new BuildFileReader();
        Model model = parser.parse(buildFile);

        File root = buildFile.getParentFile();
        if (root == null) {
            root = new File(".");
        }

        Lensfield lensfield = new Lensfield(model, root);
//        lensfield.setOffline(true);
        try {
            run(lensfield, args);
            System.err.println("----------------------------------------");
            System.err.println("BUILD COMPLETE");
        } catch (Exception e) {
            System.err.println("----------------------------------------");
            e.printStackTrace();
            System.err.println("");
            System.err.println("BUILD FAILED");
        }
    }

    private static void run(Lensfield lensfield, String[] args) throws Exception {
        if (args.length > 0) {
            if ("clean".equals(args[args.length-1])) {
                lensfield.clean();
                return;
            }
        }
        lensfield.build();
    }

}
