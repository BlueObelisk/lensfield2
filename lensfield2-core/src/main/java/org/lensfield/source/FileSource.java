/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.source;

import org.lensfield.ConfigurationException;
import org.lensfield.LensfieldException;
import org.lensfield.api.Logger;
import org.lensfield.glob.Template;
import org.lensfield.model.Parameter;
import org.lensfield.state.FileState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class FileSource implements ISource {

    private Logger LOG;

    private String name;
    private Template glob;
    private File root = new File(".");

    private List<FileState> fileList;

    private int maxFiles = -1;


    public synchronized List<FileState> run() throws IOException {
        this.fileList = new ArrayList<FileState>();
        StringBuilder s = new StringBuilder();
        recurse(root, s, 0, 0);
        return fileList;
    }

    private int recurse(File dir, StringBuilder s, int depth, int nfiles) {
        int n = s.length();
        for (String fn : dir.list()) {
            s.append(fn);
            File f = new File(dir, fn);
            if (f.isDirectory()) {
                if (depth == 0 && ".lf".equals(fn)) {
                    s.setLength(n);
                    continue;
                }
                s.append('/');
                recurse(f, s, depth+1, nfiles);
            } else {
                String path = s.toString();
                Map<String,String> params = glob.matches(path);
                if (params != null) {
                    LOG.debug(name, "adding "+path);
                    FileState fr = new FileState(s.toString(), f.lastModified(), params);
                    fileList.add(fr);
                    if (++nfiles == maxFiles) {
                        break;
                    }
                }
            }
            s.setLength(n);           
        }
        return nfiles;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setRoot(File root) {
        this.root = root;
    }

    public void setGlob(Template glob) {
        this.glob = glob;
    }

    public void setLogger(Logger logger) {
        this.LOG = logger;
    }

}
