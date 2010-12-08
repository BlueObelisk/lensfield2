package org.lensfield.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Sam Adams
 */
public class ResourceManager {

    private ConcurrentMap<String,Resource> resourceMap = new ConcurrentHashMap<String, Resource>();

    public void addResource(Resource resource) throws DuplicateResourceException {
        String path = resource.getPath();
        if (resourceMap.putIfAbsent(path, resource) != null) {
            throw new DuplicateResourceException("Duplicate resource: "+path);
        }
    }

}
