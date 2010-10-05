/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.source.atom;

import net.sf.atomxom.feeds.DefaultFeedHandler;
import net.sf.atomxom.feeds.FeedCache;
import net.sf.atomxom.model.AtomEntry;
import nu.xom.Document;
import nu.xom.Serializer;
import org.apache.commons.codec.digest.DigestUtils;
import org.lensfield.LensfieldException;
import org.lensfield.api.Logger;
import org.lensfield.glob.Glob;
import org.lensfield.glob.MissingParameterException;
import org.lensfield.model.Parameter;
import org.lensfield.source.ISource;
import org.lensfield.state.FileState;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class AtomFeed implements ISource {

    private File root;
    private Glob glob;
    private List<FileState> fileList;

    private String url;
    private String cacheDir = ".lf/feedcache";
    private int splitDirs = 1;
    private int maxEntries = -1;

    public synchronized List<FileState> run() throws Exception {
        fileList = new ArrayList<FileState>();

        FeedCache cache = new FeedCache();
        cache.setHandler(new Handler());
        cache.setCacheDir(new File(cacheDir));

        cache.setMaxEntries(maxEntries);
        cache.setSkipOldEntries(false);

        cache.poll(url);
        
        return fileList;
    }

    private class Handler extends DefaultFeedHandler {

        @Override
        public void nextEntry(AtomEntry entry) {
            try {
                Map<String,String> params = new HashMap<String,String>();
                String md5 = DigestUtils.md5Hex(entry.getId()+"/"+entry.getUpdated().toString());
                params.put("*", md5);
                params.put("**", getDir(md5));

                String path = glob.format(params);
                File file = new File(root, path);
                File dir = file.getParentFile();
                if (!dir.isDirectory()) {
                    if (!dir.mkdirs()) {
                        throw new IOException("Failed to create directory: "+dir);
                    }
                }
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                try {
                    Document doc = new Document(entry.copy());
                    Serializer s = new Serializer(out);
                    s.write(doc);
                } finally {
                    out.close();
                }
                FileState fs = new FileState(path, file.lastModified(), params);
                fileList.add(fs);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (MissingParameterException e) {
                throw new RuntimeException(e);
            }
        }

        private String getDir(String md5) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < splitDirs; i++) {
                s.append(md5.substring(2*i, (2*i)+2));
                s.append('/');
            }
            return s.toString();
        }

    }


    public void configure(List<Parameter> params) throws LensfieldException {
        for (Parameter param : params) {
            if ("max-entries".equals(param.getName())) {
                maxEntries = Integer.parseInt(param.getValue());
            }
            else if ("url".equals(param.getName())) {
                url = param.getValue();
            }
            else if ("split-dirs".equals(param.getName())) {
                splitDirs = Integer.parseInt(param.getValue());
            }
        }
    }

    public void setRoot(File root) {
        this.root = root;
    }

    public void setGlob(Glob glob) {
        this.glob = glob;
    }

    public void setLogger(Logger logger) {
        // ignore
    }

    public void setName(String id) {
        // ignore
    }
}
