/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.build;

import org.lensfield.state.FileState;
import org.lensfield.glob.Template;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class FileList {

    private Template glob;
    private List<FileState> files = new ArrayList<FileState>();

    public FileList(Template glob) {
        this.glob = glob;
    }

    public void addFile(FileState fr) {
        files.add(fr);
    }

    public List<FileState> getFiles() {
        return files;
    }

    public Template getGlob() {
        return glob;
    }

    public void addFiles(List<FileState> fileList) {
        files.addAll(fileList);
    }
}
