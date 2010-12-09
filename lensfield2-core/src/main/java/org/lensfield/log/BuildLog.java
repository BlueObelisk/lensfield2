package org.lensfield.log;

import org.joda.time.DateTime;
import org.lensfield.concurrent.Resource;

import java.util.*;

/**
 * @author Sam Adams
 */
public class BuildLog {

    private DateTime timeStarted;
    private Map<String,TaskLog> taskMap = new LinkedHashMap<String, TaskLog>();
    private Map<String, Resource> resourceMap = new HashMap<String, Resource>();


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

    public void registerResources(List<Resource> resources) {
        for (Resource resource : resources) {
            resourceMap.put(resource.getPath(), resource);
        }
    }

    public Resource getResource(String path) {
        return resourceMap.get(path);
    }
}
