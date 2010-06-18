/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.io;

import org.lensfield.api.Logger;
import org.lensfield.build.FileList;
import org.lensfield.glob.Template;
import org.lensfield.state.FileState;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author sea36
 */
public class FileSource {

    private Logger LOG;

    private String name;
    private Template glob;
    private String root = "./";

    private FileList fileList;


    public FileSource(String name, File root, Template glob) {
        this.name = name;
        this.root = root.getPath();
        this.glob = glob;
    }

    public synchronized FileList run() throws IOException {
        this.fileList = new FileList(glob);
        File rootDir = new File(root);
        StringBuilder s = new StringBuilder();
        recurse(rootDir, s, 0);
        return fileList;
    }

    private void recurse(File dir, StringBuilder s, int depth) {
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
                recurse(f, s, depth+1);
            } else {
                String path = s.toString();
                Map<String,String> params = glob.matches(path);
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
