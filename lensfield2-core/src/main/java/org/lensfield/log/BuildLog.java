package org.lensfield.log;

import org.joda.time.DateTime;
import org.lensfield.concurrent.DuplicateResourceException;
import org.lensfield.concurrent.Resource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Sam Adams
 */
public class BuildLog {

    private DateTime timeStarted;
    private Map<String,TaskLog> taskMap = new LinkedHashMap<String, TaskLog>();
    private ConcurrentMap<String, Resource> resourceMap = new ConcurrentHashMap<String, Resource>();


    public DateTime getTimeStarted() {
        return timeStarted;
    }

    public void setTimeStarted(DateTime timeStarted) {
        this.timeStarted = timeStarted;
    }

    
    public void addTask(TaskLog task) {
        this.taskMap.put(task.getName(), task);
    }

    public TaskLog getTask(String taskName) {
        return taskMap.get(taskName);
    }

    public List<TaskLog> getTaskList() {
        return new ArrayList<TaskLog>(taskMap.values());
    }

    public void registerResources(List<Resource> resources) throws DuplicateResourceException {
        for (Resource resource : resources) {
            if (resourceMap.putIfAbsent(resource.getPath(), resource) != null) {
                throw new DuplicateResourceException(resource.getPath());
            }
        }
    }

    public Resource getResource(String path) {
        return resourceMap.get(path);
    }
}
