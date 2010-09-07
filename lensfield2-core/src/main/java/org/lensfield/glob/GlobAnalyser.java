/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.glob;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.lensfield.InputFileSet;
import org.lensfield.build.FileList;
import org.lensfield.state.FileState;

import java.util.*;

/**
 * @author sea36
 */
public class GlobAnalyser {

    private static void run(String... s) {
        Glob[] globs = new Glob[s.length];
        for (int i = 0; i < s.length; i++) {
            globs[i] = new Glob(s[i]);
        }
        areAllGroupsCommon(globs);
    }

    public static boolean areAllGroupsCommon(Glob... globs) {
        Set<String> common = new LinkedHashSet<String>(globs[0].getGroupNames());
        for (int i = 1; i < globs.length; i++) {
            List<String> groups = new ArrayList<String>(globs[i].getGroupNames());
            if (common.size() != groups.size() || !common.containsAll(groups)) {
                return false;
            }
        }
        return true;
    }

    public static Set<String> getCommonGroups(Glob... globs) {
        Set<String> common = new LinkedHashSet<String>(globs[0].getGroupNames());
        for (int i = 1; i < globs.length; i++) {
            List<String> groups = new ArrayList<String>(globs[i].getGroupNames());
            common.retainAll(groups);
        }
        return common;
    }



    public static List<InputFileSet> getInputFileSets(Map<String, FileList> inputs, Set<String> commonGroups) {

        if (commonGroups.isEmpty()) {
            Map<String,Collection<FileState>> inputMap = new HashMap<String, Collection<FileState>>();
            for (Map.Entry<String,FileList> e : inputs.entrySet()) {
                inputMap.put(e.getKey(), e.getValue().getFiles());
            }
            InputFileSet input = new InputFileSet(Collections.<String, String>emptyMap(), inputMap);
            return Collections.singletonList(input);
        }

        String[] groups = commonGroups.toArray(new String[commonGroups.size()]);

        Map<GlobMatch,ListMultimap<String,FileState>> globMatches = new LinkedHashMap<GlobMatch,ListMultimap<String, FileState>>();
        for (Map.Entry<String,FileList> e : inputs.entrySet()) {
            String inputName = e.getKey();
            for (FileState file : e.getValue().getFiles()) {
                GlobMatch glob = new GlobMatch(groups, file);
                ListMultimap<String,FileState> map = globMatches.get(glob);
                if (map == null) {
                    map = ArrayListMultimap.create();
                    globMatches.put(glob,map);
                }
                map.put(inputName, file);
            }
        }

        return createFileSets(globMatches);


        /*
        int inputCount = inputs.size();

        String[] inputNames = new String[inputs.size()];
        FileList[] fileLists = new FileList[inputs.size()];
        int ix = 0;
        for (Map.Entry<String,FileList> e : inputs.entrySet()) {
            inputNames[ix] = e.getKey();
            fileLists[ix] = e.getValue();
            ix++;
        }

        Map<GlobMatch,FileState[]> inputFiles = new LinkedHashMap<GlobMatch, FileState[]>();
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

        return createFileSets(inputNames, inputFiles);
        */
    }


    public static List<InputFileSet> getInputFileSets(Map<String, FileList> inputs) {

        Set<String> commonGroups = getCommonGroupNames(inputs);
        return getInputFileSets(inputs, commonGroups);

    }


    private static List<InputFileSet> createFileSets(Map<GlobMatch, ListMultimap<String, FileState>> globMatches) {
        List<InputFileSet> list = new ArrayList<InputFileSet>(globMatches.size());
        for (Map.Entry<GlobMatch,ListMultimap<String,FileState>> e : globMatches.entrySet()) {
            GlobMatch gm = e.getKey();
            ListMultimap<String,FileState> inputFiles = e.getValue();
            Map<String,String> params = gm.getMap();
            list.add(new InputFileSet(params, inputFiles.asMap()));
        }
        return list;
    }


    private static List<InputFileSet> createFileSets(String[] inputNames, Map<GlobMatch, FileState[]> inputFiles) {
        List<InputFileSet> list = new ArrayList<InputFileSet>(inputFiles.size());
        for (Map.Entry<GlobMatch, FileState[]> e : inputFiles.entrySet()) {
            GlobMatch gm = e.getKey();
            FileState[] files = e.getValue();
            Map<String,String> params = gm.getMap();

            Map<String,Collection<FileState>> inputMap;
            if (inputNames.length == 1) {
                inputMap = Collections.singletonMap(inputNames[0], (Collection<FileState>)Collections.singletonList(files[0]));
            } else {
                inputMap = new LinkedHashMap<String, Collection<FileState>>();
                for (int i = 0; i < inputNames.length; i++) {
                    inputMap.put(inputNames[i], Collections.singletonList(files[i]));
                }
            }
            list.add(new InputFileSet(params, inputMap));
        }
        return list;
    }

    private static Set<String> getCommonGroupNames(Map<String, FileList> inputs) {
        Iterator<FileList> it = inputs.values().iterator();
        Set<String> names = new LinkedHashSet<String>(it.next().getGlob().getGroupNames());
        while (it.hasNext()) {
            List<String> ns = it.next().getGlob().getGroupNames();
            if (names.size() != ns.size() || !names.containsAll(ns)) {
                throw new RuntimeException();   // TODO
            }
        }
        return names;
    }

    private static Glob[] getGlobs(FileList[] inputs) {
        Glob[] globs = new Glob[inputs.length];
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
