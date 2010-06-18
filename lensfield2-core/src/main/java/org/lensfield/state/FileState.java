/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import org.lensfield.build.OutputDescription;
import org.lensfield.glob.Template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sea36
 */
public class FileState {

    private String path;
    private long lastModified;
    private Map<String,String> params;

    public FileState(String path, long lastModified, Map<String,String> params) {
        this.path = path;
        this.lastModified = lastModified;
        this.params = Collections.unmodifiableMap(params);
    }

    public String getPath() {
        return path;
    }

    public String getParam(String key) {
        return getParams().get(key);
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        if (this.params != null) {
            throw new IllegalStateException("params already set");
        }
        this.params = params;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "["+path+" "+params+"]";
    }

}
