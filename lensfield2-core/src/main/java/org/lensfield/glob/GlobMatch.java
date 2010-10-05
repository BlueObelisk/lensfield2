/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.glob;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author sea36
 */
public class GlobMatch {

    private final String[] groups;
    private final String[] values;
    private int hash;

//    public GlobMatch(String[] groups, FileState file) {
//        this.groups = groups;
//        this.values = new String[groups.length];
//        for (int i = 0; i < groups.length; i++) {
//            values[i] = file.getParam(groups[i]);
//        }
//    }

    GlobMatch(String[] groups, String[] values) {
        this.groups = groups;
        this.values = values;
    }

    public Map<String,String> getMap() {
        Map<String,String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i < groups.length; i++) {
            map.put(groups[i], values[i]);
        }
        return map;
    }


    @Override
    public int hashCode() {
        if (hash == 0) {
            int h = 2377 * Arrays.hashCode(values);
            hash = h;
        }
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GlobMatch) {
            GlobMatch globMatch = (GlobMatch) o;
            return Arrays.equals(values, globMatch.values);
        }
        return false;
    }

    @Override
    public String toString() {
        return "["+Arrays.asList(values)+"]";
    }
}
