/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.source;

import org.lensfield.ConfigurationException;
import org.lensfield.LensfieldException;
import org.lensfield.concurrent.Resource;
import org.lensfield.glob.Glob;
import org.lensfield.glob.GlobMatch;
import org.lensfield.model.Parameter;
import org.lensfield.state.OutputPipe;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author sea36
 */
public class FileSource {

    private String glob = "**/*";
    private String root = ".";

    private Glob globTemplate;
    private File rootDir = new File(".");

    private int maxFiles = -1;

    private OutputPipe output;

    public synchronized void run() throws IOException {
        if (glob == null || root == null || output == null) {
            throw new IllegalStateException("file source not configured");
        }
        this.globTemplate = new Glob(glob);
        this.rootDir = new File(root);
        StringBuilder s = new StringBuilder();
        recurse(rootDir, s, 0, 0);
    }

    private int recurse(File dir, StringBuilder s, int depth, int nfiles) {
        int n = s.length();
        for (String fn : dir.list()) {
            s.append(fn);
            File f = new File(dir, fn);
            if (f.isDirectory()) {
                // TODO directory matches
                // TODO glob segment matches
                if (depth == 0 && ".lf".equals(fn)) {
                    s.setLength(n);
                    continue;
                }
                s.append('/');
                recurse(f, s, depth+1, nfiles);
            } else {
                String path = s.toString();
                GlobMatch match = globTemplate.match(path);
                if (match != null) {
                    Resource resource = new Resource(path, f, match.getMap());
                    output.sendResource(resource);
                    if (++nfiles == maxFiles) {
                        break;
                    }
                }
            }
            s.setLength(n);
        }
        return nfiles;
    }

    public void configure(List<Parameter> params) throws LensfieldException {
        for (Parameter param : params) {
            if ("max-files".equals(param.getName())) {
                maxFiles = Integer.parseInt(param.getValue());
            }
            else {
                throw new ConfigurationException("Unknown parameter: "+param.getName());
            }
        }
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public void setGlob(String glob) {
        this.glob = glob;
    }

    public void setOutput(OutputPipe output) {
        this.output = output;
    }

}
