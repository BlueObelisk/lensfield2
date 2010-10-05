package org.lensfield.concurrent;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * @author sea36
 */
public class Resource {

    private String path;
    private long lastModified;
    private Map<String,String> parameters;

    private File file;

    private boolean existing;
    private boolean outOfDate;
    

    public Resource(String path, File f, Map<String, String> map) {
        this.path = path;
        this.lastModified = f.lastModified();
        this.parameters = map;
        this.file = f;
    }

    public Resource(String path, long lastModified, Map<String, String> map) {
        this.path = path;
        this.lastModified = lastModified;
        this.parameters = map;
    }

    public String getPath() {
        return path;
    }

    public long getLastModified() {
        return lastModified;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public File getFile() {
        return file;
    }
}
