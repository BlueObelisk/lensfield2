/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import java.util.*;

/**
 * @author sea36
 */
public class BuildState {

    private Map<String,TaskState> taskMap = new HashMap<String,TaskState>();
    private long started = System.currentTimeMillis();

    private Map<String,FileState> outputFileMap = new HashMap<String, FileState>();    

    public long getStarted() {
        return started;
    }

    public void addTask(TaskState task) {
        taskMap.put(task.getId(), task);
    }

    public TaskState getTask(String id) {
        return taskMap.get(id);
    }

    public List<TaskState> getTasks() {
        return new ArrayList<TaskState>(taskMap.values());
    }

    public void setStarted(long started) {
        this.started = started;
    }



    public void addFiles(Collection<FileState> files) {
        for (FileState f : files) {
            outputFileMap.put(f.getPath(),f);
        }
    }

    public Map<String, FileState> getOutputFiles() {
        return outputFileMap;
    }

}
