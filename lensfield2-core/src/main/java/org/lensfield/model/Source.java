/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.model;

/**
 * @author sea36
 */
public class Source extends Process {

    private String template;

    public Source(String name) {
        super(name);
    }

    public Source(String name, String glob) {
        super(name);
        setTemplate(glob);
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

}
