/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import java.io.File;

/**
 * @author sea36
 */
public class Dependency {

    private String id;
    private long lastModified;

    private transient File file;

    public Dependency(String id, long lastModified) {
        this.id = id;
        this.lastModified = lastModified;
    }

    public Dependency(String id, File file) {
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

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Dependency) {
            Dependency other = (Dependency) o;
            return getId().equals(other.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
