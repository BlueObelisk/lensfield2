/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import org.lensfield.api.Logger;
import org.lensfield.state.FileState;
import org.lensfield.build.FileList;
import org.lensfield.glob.Template;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author sea36
 */
public class FileSource {

    private Logger LOG;

    private String name;
    private String filter;
    private String root = "./";

    private FileList fileList;


    public FileSource(String name, File root, String filter) {
        this.name = name;
        this.root = root.getPath();
        this.filter = filter;
    }

    public synchronized FileList run() throws IOException {
        this.fileList = new FileList(new Template(filter));
        File rootDir = new File(root);
        Template template = new Template(filter);
        StringBuilder s = new StringBuilder();
        recurse(rootDir, s, template, 0);
        return fileList;
    }

    private void recurse(File dir, StringBuilder s, Template template, int depth) {
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
                recurse(f, s, template, depth+1);
            } else {
                String path = s.toString();
                Map<String,String> params = template.matches(path);
                if (params != null) {
                    LOG.debug(name, "adding "+path);
                    FileState fr = new FileState(s.toString(), f.lastModified(), params);
                    fileList.addFile(fr);
                }
            }
            s.setLength(n);           
        }
    }

    public void setLogger(Logger logger) {
        this.LOG = logger;
    }
}
