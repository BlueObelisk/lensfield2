package org.lensfield.model;

/**
 * @author sea36
 */
public abstract class Resource {

    private final String name;

    protected Resource() {
        this.name = null;
    }

    protected Resource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
