package org.lensfield.glob;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author sea36
 */
public class GlobMatcher {

    private Glob glob;
    private List<String> list;

    public GlobMatcher(String glob) {
        this(new Glob(glob));
    }

    public GlobMatcher(Glob glob) {
        this.glob = glob;
    }

    public synchronized List<String> find(File root) {

        list = new ArrayList<String>();
        try {
            StringBuilder path = new StringBuilder();
            match(root, path, 0);
            return list;
        } finally {
            list = null;
        }
    }


    private void match(File root, StringBuilder path, int i) {
        int pathLen = path.length();

        GlobSegment seg = glob.getSegment(i);

        if (seg.isWildDir()) {
            // TODO check directory matches wildcard dir before looking at files
            File d = new File(root, path.toString());
            for (File f : d.listFiles()) {
                path.append(f.getName());
                System.err.println(path);
                if (f.isFile()) {
                    if (glob.matches(path.toString())) {
                        match(f, path.toString());
                    }
                } else if (f.isDirectory()) {
                    path.append('/');
                    match(root, path, i);
                }
                path.setLength(pathLen);
            }
        } else if (seg.isWildcard()) {
            File d = new File(root, path.toString());
            for (File f : d.listFiles()) {
                if (seg.isLast()) {
                    if (f.isFile() && seg.matches(f.getName())) {
                        path.append(f.getName());
                        match(f, path.toString());
                        path.setLength(pathLen);
                    }
                } else {
                    if (f.isDirectory() && seg.matches(f.getName())) {
                        path.append(f.getName()).append('/');
                        match(root, path, i+1);
                        path.setLength(pathLen);
                    }
                }
            }
        } else {
            path.append(seg.getPath());
            String p = path.toString();
            File f = new File(root, p);
//            System.err.println(p+"\t"+f+"\t"+f.isFile());
            if (seg.isLast()) {
                if (f.isFile()) {
                    match(f, p);
                }
            } else {
                if (f.isDirectory()) {
                    if (path.charAt(path.length()-1) != '/') {
                        path.append('/');
                    }
                    match(root, path, i+1);
                }
            }
            path.setLength(pathLen);
        }
    }

    private void match(File f, String p) {
        list.add(p);
    }

}
