package org.lensfield.concurrent;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.lensfield.log.OperationLog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author sea36
 */
public class Resource {

    private final String path;
    private long lastModified = -1;
    private Map<String,String> parameters;

    private File file;

    private OperationLog producer;
    private boolean updated = true;

    public Resource(String path, File f, Map<String, String> map) {
        this.path = path;
        this.parameters = map;
        this.file = f;
        if (f.isFile()) {
            lastModified = f.lastModified();
        }
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

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public File getFile() {
        return file;
    }


    public String calculateMd5() throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            return DigestUtils.md5Hex(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }


    public OperationLog getProducer() {
        return producer;
    }

    public void setProducer(OperationLog producer) {
        this.producer = producer;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public boolean isUpdated() {
        return updated;
    }
}
