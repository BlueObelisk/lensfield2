package org.lensfield.log;

import org.lensfield.concurrent.Resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sam Adams
 */
public class OperationLog {

    private Map<String,List<Resource>> inputMap = new LinkedHashMap<String, List<Resource>>();
    private Map<String,List<Resource>> outputResourcesMap = new LinkedHashMap<String, List<Resource>>();

    public void addOutput(String name, List<Resource> resources) {
        outputResourcesMap.put(name, resources);
    }

    public boolean hasInputs() {
        return !inputMap.isEmpty();
    }

    public List<List<Resource>> getOutputSets() {
        return new ArrayList<List<Resource>>(outputResourcesMap.values());
    }
    
}
