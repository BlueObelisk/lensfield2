package org.lensfield.concurrent;

import java.util.Collections;
import java.util.Map;

/**
 * @author sea36
 */
public class ParameterSet {

    private Map<String,String> map;

    public ParameterSet(Map<String, String> map) {
        if (map == null) {
            throw new IllegalArgumentException("null argument");
        }
        this.map = Collections.unmodifiableMap(map);
    }

    public Map<String,String> toMap() {
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ParameterSet) {
            ParameterSet that = (ParameterSet) o;
            return map.equals(that.map);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
    
}
