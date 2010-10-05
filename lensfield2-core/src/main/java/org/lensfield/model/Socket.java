package org.lensfield.model;

/**
 * @author sea36
 */
public abstract class Socket {

    private final String name;

    protected Socket() {
        this.name = null;
    }

    protected Socket(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
