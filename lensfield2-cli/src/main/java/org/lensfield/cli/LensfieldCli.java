/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.cli;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.lensfield.BuildFileParser;
import org.lensfield.Lensfield;
import org.lensfield.model.Model;

import java.io.*;

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
        main(args, null);
    }


    public static void main(String[] args, ClassWorld classworld) throws Exception {

        String version = loadVersion();

        System.err.println("Lensfield2 ("+version+")");
        System.err.println("----------------------------------------");

        // Locate build file
        File buildFile = null;
        if (args.length == 0) {
            buildFile = new File("build.lf");
        } else if (args.length == 1) {
            buildFile = new File(args[0]);
            if (buildFile.isDirectory()) {
                buildFile = new File(buildFile, "build.lf");
            }
        } else {
            System.err.println("Error parsing command line.");
            System.err.println("Usage:");
            System.err.println("  lf [build path]");
            System.exit(1);
        }

        if (!buildFile.isFile()) {
            System.err.println(" ** ERROR ** 'build.lf' not found");
            System.exit(1);
        }

        // Parse build file
        BuildFileParser parser = new BuildFileParser();
        Model model = parser.parse(buildFile);

        File root = buildFile.getParentFile();
        if (root == null) {
            root = new File(".");
        }

        Lensfield lensfield = new Lensfield(model, root, classworld);
        lensfield.build();

        System.err.println("----------------------------------------");
        System.err.println("BUILD COMPLETE");

    }

}
