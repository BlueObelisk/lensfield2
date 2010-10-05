package org.lensfield.concurrent;

import org.lensfield.state.Input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author sea36
 */
public class ResourceSet {

    private List<Resource> list = new ArrayList<Resource>();
    private Input input;

    public ResourceSet(Input input) {
        this.input = input;
    }

    public void add(Resource r) {
        if (list.isEmpty() || input.isMultifile()) {
            list.add(r);
        } else {
            throw new IllegalStateException();
        }
    }

    public List<Resource> list() {
        return list;
    }

    public boolean isReady() {
        return input.isClosed() || (!input.isMultifile() && list.size() == 1);
    }
}
