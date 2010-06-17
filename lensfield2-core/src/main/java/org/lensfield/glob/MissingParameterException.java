package org.lensfield.glob;

/**
 * @author sea36
 */
public class MissingParameterException extends Exception {

    private String name;

    public MissingParameterException(String name) {
        super("Missing parameter: "+name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
}
