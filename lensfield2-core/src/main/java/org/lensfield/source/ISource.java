package org.lensfield.source;

import org.lensfield.LensfieldException;
import org.lensfield.api.Logger;
import org.lensfield.glob.Template;
import org.lensfield.model.Parameter;
import org.lensfield.state.FileState;

import java.io.File;
import java.util.List;

/**
 * @author sea36
 */
public interface ISource {

    void configure(List<Parameter> params) throws LensfieldException;

    void setRoot(File root);

    void setGlob(Template glob);

    void setLogger(Logger logger);

    List<FileState> run() throws Exception;

    void setName(String id);
}
