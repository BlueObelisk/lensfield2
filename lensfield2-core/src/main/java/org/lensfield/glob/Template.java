/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.glob;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author sea36
 */
public class Template {

    private Pattern pattern;

    private String glob;
    private String formatPattern;

    private LinkedHashMap<String,Integer> groupNames = new LinkedHashMap<String, Integer>();

    private boolean wildcard;

    public Template(String glob) {
        this.glob = glob;
        parseGlob();
    }

    public String getGlob() {
        return glob;
    }

    public List<String> getGroupNames() {
        return new ArrayList<String>(groupNames.keySet());
    }

    private void parseGlob() {
        StringBuilder ptrn = new StringBuilder();
        StringBuilder fptrn = new StringBuilder();
        boolean newDir = true;
        int len = glob.length();

        for (int i = 0; i < len;) {
            char c = glob.charAt(i++);

            switch (c) {

                case '*':
                    if (i < len && '*' == glob.charAt(i)) {
                        if (!newDir) {
                            throw new IllegalArgumentException("glob error '/**/'");
                        }
                        i++;
                        if (i == len || '/' != glob.charAt(i)) {
                            throw new IllegalArgumentException("glob error '/**/'");
                        }
                        i++;
                        if (groupNames.containsKey("**")) {
                            throw new IllegalArgumentException("Duplicate wildcard: **");
                        }
                        int x = groupNames.size();
                        groupNames.put("**", x);
                        ptrn.append("(.*/|)");
                        fptrn.append('{').append(x).append('}');
                        wildcard = true;
                    } else {
                        if (groupNames.containsKey("*")) {
                            throw new IllegalArgumentException("Duplicate wildcard: *");
                        }
                        int x = groupNames.size();
                        groupNames.put("*", x);
                        ptrn.append("([^/]*)");
                        fptrn.append('{').append(x).append('}');
                        wildcard = true;
                    }
                    break;

                case '{':
                    int i0 = i;
                    do {
                        i++;
                    } while (glob.charAt(i) != '}');
                    String name = glob.substring(i0, i);
                    if (groupNames.containsKey(name)) {
                        throw new IllegalArgumentException("Duplicate wildcard: *");
                    }
                    int x = groupNames.size();
                    groupNames.put(name, x);
                    ptrn.append("([^/]*)");
                    fptrn.append("{").append(x).append("}");
                    wildcard = true;
                    i++;
                    break;

                case '/':
                    ptrn.append(c);
                    fptrn.append(c);
                    newDir = true;
                    break;

                case '\\':  // TODO e.g. abc\*def

                case '?':
                case '$':
                case '^':
                case '(':
                case ')':
                case '[':
                case ']':
                case '<':
                case '>':
                case '.':
                case '+':
                case '|':
                    ptrn.append('\\');
                default:
                    ptrn.append(c);
                    fptrn.append(c);
                    newDir = false;
            }

        }
        formatPattern = fptrn.toString();
        pattern = Pattern.compile(ptrn.toString());
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public Map<String,String> matches(String path) {
        Matcher m = pattern.matcher(path);
        if (m.matches()) {
            Map<String,String> map = new HashMap<String, String>();
            Iterator<String> names = groupNames.keySet().iterator();
            for (int i = 0; i < groupNames.size(); i++) {
                map.put(names.next(), m.group(i+1));
            }
            return map;
        } else {
            return null;
        }
    }

    public String format(Map<String,String> params) throws MissingParameterException {
        String p = formatPattern;
        for (Map.Entry<String,Integer> e : groupNames.entrySet()) {
            String name = e.getKey();
            Integer i = e.getValue();
            String value = params.get(name);
            if (value == null) {
                throw new MissingParameterException(name);
            }
            p = p.replace("{"+i+"}", value);
        }
//        System.err.println(p);
        return p;
    }

}
