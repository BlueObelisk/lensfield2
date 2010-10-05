/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sea36
 */
public class BuildState {

    private Map<String, Process> taskMap = new HashMap<String, Process>();
    private long started = System.currentTimeMillis();

    public long getStarted() {
        return started;
    }

    public void addTask(Process task) {
        taskMap.put(task.getId(), task);
    }

    public Process getTask(String id) {
        return taskMap.get(id);
    }

    public List<Process> getTasks() {
        return new ArrayList<Process>(taskMap.values());
    }

    public void setStarted(long started) {
        this.started = started;
    }

}
