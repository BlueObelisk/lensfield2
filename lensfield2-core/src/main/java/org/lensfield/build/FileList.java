/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.build;

import org.lensfield.glob.Glob;
import org.lensfield.state.FileState;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sea36
 */
public class FileList {

    private Glob glob;
    private List<FileState> files = new ArrayList<FileState>();

    public FileList(Glob glob) {
        this.glob = glob;
    }

    public void addFile(FileState fr) {
        files.add(fr);
    }

    public List<FileState> getFiles() {
        return files;
    }

    public Glob getGlob() {
        return glob;
    }

    public void addFiles(List<FileState> fileList) {
        files.addAll(fileList);
    }
}
