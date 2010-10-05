package org.lensfield.state;

import org.lensfield.concurrent.ParameterSet;

/**
 * @author sea36
 */
public class OperationKey {

    private Process process;
    private ParameterSet parameterSet;

    public OperationKey(Process process, ParameterSet parameterSet) {
        if (process == null || parameterSet == null) {
            throw new IllegalArgumentException("Null argument");
        }
        this.process = process;
        this.parameterSet = parameterSet;
    }

    public Process getProcess() {
        return process;
    }

    public ParameterSet getParameterSet() {
        return parameterSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof OperationKey) {
            OperationKey that = (OperationKey) o;
            return process.equals(that.process) && parameterSet.equals(that.parameterSet);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = process.hashCode()*31 + parameterSet.hashCode();
        return result;
    }

}
