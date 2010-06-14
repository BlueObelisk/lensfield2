/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.model;

import java.io.File;

/**
 * @author sea36
 */
public class Dependency {

    private final String id;
    private final String groupId;
    private final String artifactId;
    private final String version;

    private long lastModified;
    private File file;

    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.id = groupId+':'+artifactId+':'+version;
    }

    public String getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
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
