/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sea36
 */
public class FileState {

    private String path;
    private long lastModified;
    private Map<String,String> params = new HashMap<String, String>();

    protected FileState() {
        
    }

    public FileState(String path, long lastModified, Map<String, String> params) {
        this.path = path;
        this.lastModified = lastModified;
        this.params.putAll(params);
    }

    public FileState(String path, long lastModified) {
        this.path = path;
        this.lastModified = lastModified;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParam(String key) {
        return params.get(key);
    }

    public void setParam(String key, String value) {
        params.put(key, value);
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
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

