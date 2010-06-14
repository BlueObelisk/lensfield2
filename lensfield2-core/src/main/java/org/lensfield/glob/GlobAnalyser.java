/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.glob;

import org.lensfield.FileSet;
import org.lensfield.state.FileState;
import org.lensfield.build.FileList;

import java.util.*;

/**
 * @author sea36
 */
public class GlobAnalyser {

    private static void run(String... s) {
        Template[] globs = new Template[s.length];
        for (int i = 0; i < s.length; i++) {
            globs[i] = new Template(s[i]);
        }
        allGroupsCommon(globs);
    }

    private static boolean allGroupsCommon(Template... globs) {
        Set<String> common = new HashSet<String>(globs[0].getGroupNames());
        for (int i = 1; i < globs.length; i++) {
            List<String> groups = new ArrayList<String>(globs[i].getGroupNames());
            if (common.size() != groups.size() || !common.containsAll(groups)) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> getCommonGroups(Template... globs) {
        Set<String> common = new HashSet<String>(globs[0].getGroupNames());
        for (int i = 1; i < globs.length; i++) {
            List<String> groups = new ArrayList<String>(globs[i].getGroupNames());
            common.retainAll(groups);
        }
        return common;
    }



    public static List<FileSet> getInputFileSets(Map<String, FileList> inputs) {
        int inputCount = inputs.size();

        String[] groups = getCommonGroupNames(inputs);

        String[] inputNames = new String[inputs.size()];
        FileList[] fileLists = new FileList[inputs.size()];
        int ix = 0;
        for (Map.Entry<String,FileList> e : inputs.entrySet()) {
            inputNames[ix] = e.getKey();
            fileLists[ix] = e.getValue();
            ix++;
        }

        Map<GlobMatch,FileState[]> inputFiles = new HashMap<GlobMatch, FileState[]>();
        for (FileState file : fileLists[0].getFiles()) {
            FileState[] files = new FileState[inputCount];
            files[0] = file;
            GlobMatch glob = new GlobMatch(groups, file);
            inputFiles.put(glob, files);
        }

        for (int i = 1; i < inputCount; i++) {
            for (FileState file : fileLists[i].getFiles()) {
                GlobMatch glob = new GlobMatch(groups, file);
                FileState[] files = inputFiles.get(glob);
                files[i] = file;
            }
        }

        List<FileSet> fileSets = createFileSets(inputNames, inputFiles);
        return fileSets;
    }

    private static List<FileSet> createFileSets(String[] inputNames, Map<GlobMatch, FileState[]> inputFiles) {
        List<FileSet> list = new ArrayList<FileSet>(inputFiles.size());
        for (Map.Entry<GlobMatch, FileState[]> e : inputFiles.entrySet()) {
            GlobMatch gm = e.getKey();
            FileState[] files = e.getValue();
            Map<String,String> params = gm.getMap();

            Map<String,List<FileState>> inputMap;
            if (inputNames.length == 1) {
                inputMap = Collections.singletonMap(inputNames[0], Collections.singletonList(files[0]));
            } else {
                inputMap = new HashMap<String, List<FileState>>();
                for (int i = 0; i < inputNames.length; i++) {
                    inputMap.put(inputNames[i], Collections.singletonList(files[i]));
                }
            }
            list.add(new FileSet(params, inputMap));
        }
        return list;
    }

    private static String[] getCommonGroupNames(Map<String, FileList> inputs) {
        Iterator<FileList> it = inputs.values().iterator();
        Set<String> names = new LinkedHashSet<String>(it.next().getGlob().getGroupNames());
        while (it.hasNext()) {
            List<String> ns = it.next().getGlob().getGroupNames();
            if (names.size() != ns.size() || !names.containsAll(ns)) {
                throw new RuntimeException();   // TODO
            }
        }
        return names.toArray(new String[names.size()]);
    }

    private static Template[] getGlobs(FileList[] inputs) {
        Template[] globs = new Template[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            globs[i] = inputs[i].getGlob();
        }
        return globs;
    }


    public static void main(String[] args) {
        run("**/*", "**/*");
        run("**/*", "**/x");
    }


}
