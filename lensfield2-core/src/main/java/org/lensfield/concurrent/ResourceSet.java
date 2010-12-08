package org.lensfield.concurrent;

import org.lensfield.state.InputPipe;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sea36
 */
public class ResourceSet {

    private List<Resource> list = new ArrayList<Resource>();
    private InputPipe input;

    public ResourceSet(InputPipe input) {
        this.input = input;
    }

    public void addResource(Resource r) {
        if (list.isEmpty() || input.isMultifile()) {
            list.add(r);
        } else {
            throw new IllegalStateException();
        }
    }

    public List<Resource> getResourceList() {
        return list;
    }

    public boolean isReadyAsInput() {
        return input.isClosed() || (!input.isMultifile() && list.size() == 1);
    }
    
}
