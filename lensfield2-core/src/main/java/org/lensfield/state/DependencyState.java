/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import java.io.File;

/**
 * @author sea36
 */
public class DependencyState {

    private String id;
    private long lastModified;

    private transient File file;

    public DependencyState(String id, long lastModified) {
        this.id = id;
        this.lastModified = lastModified;
    }

    public DependencyState(String id, File file) {
        this.id = id;
        this.file = file;
        this.lastModified = file.lastModified();
        lastModified = 1000 * (lastModified/1000);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

}
