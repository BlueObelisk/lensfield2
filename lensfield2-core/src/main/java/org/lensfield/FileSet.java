/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.state.FileState;

import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class FileSet {

    private Map<String,String> parameters;
    private Map<String,List<FileState>> inputs;
    private boolean upToDate;

    public FileSet(Map<String, String> params, Map<String, List<FileState>> inputMap) {
        this.parameters = params;
        this.inputs = inputMap;
    }

    public Map<String,String> getParameters() {
        return parameters;
    }


//    public static List<FileSet> create(String[] inputs, Map<GlobMatch, FileState[]> map) {
//        List<FileSet> list = new ArrayList<FileSet>(map.size());
//        for (Map.Entry<GlobMatch, FileState[]> e : map.entrySet()) {
//            list.add(new FileSet(e.getKey(), inputs, e.getValue()));
//        }
//        return list;
//    }

    public Map<String, List<FileState>> getMap() {
        return inputs;
    }

    public void setUpToDate(boolean b) {
        this.upToDate = b;
    }
    
    public boolean isUpToDate() {
        return upToDate;
    }
}
