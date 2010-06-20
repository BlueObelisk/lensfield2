package org.lensfield.glob;

import org.apache.commons.io.IOCase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author sea36
 */
public class Glob {

    private final String glob;
    private Pattern pattern;

    private List<GlobSegment> segments = new ArrayList<GlobSegment>();
    private LinkedHashMap<String,Integer> groupNames = new LinkedHashMap<String, Integer>();

    public Glob(String glob) {
        this.glob = glob;
        parseGlob();
    }

    private void parseGlob() {

        StringBuilder formatPattern = new StringBuilder();
        StringBuilder pattern = new StringBuilder();
        StringBuilder segPattern = new StringBuilder();

        boolean wildcardDir = false;
        boolean wildcardSeg = false;

        boolean escapeNext = false;
        boolean newDir = true;
        int segStart = 0;
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (escapeNext) {
                if (escape(c)) {
                    segPattern.append('\\');
                }
                segPattern.append(c);
                formatPattern.append(c);
                escapeNext = false;
            } else {
                switch (c) {
                    case '*':
                        if ((i+1) < glob.length() && glob.charAt(i+1) == '*') {
                            if (!newDir) {
                                throw new IllegalArgumentException("Directory wildcard '**' cannot be prefixed");
                            }
                            if ((i+2) >= glob.length() || glob.charAt(i+2) != '/') {
                                throw new IllegalArgumentException("Directory wildcard '**' cannot be suffixed");
                            }
                            if (groupNames.containsKey("**")) {
                                throw new IllegalArgumentException("Only a single directory wildcard '**' is allowed");
                            }
                            i += 2;
                            int x = groupNames.size();
                            groupNames.put("**", x);
                            segPattern.append("(.*/|)");
                            formatPattern.append('<').append(Integer.toString(x)).append('>');
                            wildcardDir = true;
                        }
                        else {
                            if (groupNames.containsKey("*")) {
                                throw new IllegalArgumentException("Only a single file wildcard '*' is allowed");
                            }
                            int x = groupNames.size();
                            groupNames.put("*", x);
                            segPattern.append("([^/]*)");
                            formatPattern.append('<').append(Integer.toString(x)).append('>');
                            wildcardSeg = true;
                        }
                        break;

                    case '{':
                        segPattern.append("[^/]*");
                        wildcardSeg = true;
                        int i0 = glob.indexOf('}', i);
                        if (i0 == -1) {
                            throw new IllegalArgumentException("Bad wildcard - no closing '}'");
                        }
                        String n = glob.substring(i+1, i0);
                        if (groupNames.containsKey(n)) {
                            throw new IllegalArgumentException("Only a single file wildcard '"+n+"' is allowed");
                        }
                        int x = groupNames.size();
                        groupNames.put(n, x);
                        segPattern.append("([^/]*)");
                        formatPattern.append('<').append(Integer.toString(x)).append('>');
                        wildcardSeg = true;
                        i = i0+1;
                        break;

                    case '/':
                        if (newDir) {
                            // collapse '//+'
                            continue;
                        }
                        if (!wildcardDir) {
                            pattern.append(segPattern).append('/');
                            GlobSegment seg = new GlobSegment(glob.substring(segStart,i+1), Pattern.compile(segPattern.toString()), wildcardSeg, wildcardDir, false);
                            segPattern.setLength(0);
                            segments.add(seg);
                            segStart = i+1;
                            wildcardSeg = false;
                            newDir = true;
                        } else {
                            segPattern.append('/');
                        }
                        formatPattern.append('/');

                        continue;

                        // Escape next character
                    case '\\':
                        if (i == glob.length()) {
                            throw new IllegalArgumentException("Bad escape character: "+glob);
                        }
                        escapeNext = true;
                        break;

                    default:
                        if (escape(c)) {
                            segPattern.append('\\');
                        }
                        segPattern.append(c);
                        formatPattern.append(c);
                }
            }
            newDir = false;
        }

        // Append last segment
        pattern.append(segPattern);

        GlobSegment seg = new GlobSegment(glob.substring(segStart), Pattern.compile(segPattern.toString()), wildcardSeg, wildcardDir, true);
        segments.add(seg);
        System.err.println(pattern.toString());
        System.err.println(formatPattern.toString());

        if (IOCase.SYSTEM.isCaseSensitive()) {
            this.pattern = Pattern.compile(pattern.toString());
        } else {
            this.pattern = Pattern.compile(pattern.toString(), Pattern.CASE_INSENSITIVE);
        }

    }

    private static boolean escape(char ch) {
        switch (ch) {
            case '(':
            case ')':
            case '[':
            case ']':
            case '{':
            case '}':
            case '.':
            case '+':
            case '*':
            case '?':
            case '\\':
            case '^':
            case '$':
            case '|':
                return true;
            default:
                return false;
        }
    }

    public GlobSegment getSegment(int i) {
        return segments.get(i);
    }

    public boolean matches(String s) {
        return pattern.matcher(s).matches();
    }
}