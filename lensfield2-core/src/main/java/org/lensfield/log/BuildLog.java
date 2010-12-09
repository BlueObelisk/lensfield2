package org.lensfield.log;

import org.joda.time.DateTime;
import org.lensfield.concurrent.DuplicateResourceException;
import org.lensfield.concurrent.Resource;
import org.lensfield.concurrent.ResourceManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sam Adams
 */
public class BuildLog {

    private DateTime timeStarted;
    private Map<String,TaskLog> taskMap = new LinkedHashMap<String, TaskLog>();

    private ResourceManager resourceManager = new ResourceManager();

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
            resourceManager.addResource(resource);
        }
    }

    public Resource getResource(String path) {
        return resourceManager.getResource(path);
    }
}
