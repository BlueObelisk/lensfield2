package org.lensfield.glob;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.comparator.PathFileComparator;

import java.util.regex.Pattern;

/**
 * @author sea36
 */
public class GlobSegment {

    private static final IOCase IOCASE = IOCase.SYSTEM;

    private String path;
    private Pattern pattern;

    private boolean wildDir;
    private boolean wildcard;
    private boolean last;


    public GlobSegment(String path, Pattern pattern, boolean wildcard, boolean wildDir, boolean last) {
        this.path = path;
        this.pattern = pattern;
        this.wildcard = wildcard;
        this.wildDir = wildDir;
        this.last = last;

//        System.err.println("--- SEGMENT -------");
//        System.err.println("  "+path+"\t["+pattern+"]\t"
//                +(last?"[last]":"")
//                +(wildcard?"[*]":"")
//                +(wildDir?"[**]":""));

    }


    public boolean matches(String pathSegment) {
        if (isWildcard() || isWildDir()) {
            return pattern.matcher(pathSegment).matches();
        } else {
            return IOCASE.checkEquals(path, pathSegment);
        }
    }

    public String getPath() {
        return path;
    }

    public boolean isWildDir() {
        return wildDir;
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public boolean isLast() {
        return last;
    }

}
