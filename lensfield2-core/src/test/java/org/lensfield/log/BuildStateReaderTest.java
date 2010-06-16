/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.log;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author sea36
 */
public class BuildStateReaderTest {

    @Test
    public void testParseBuildLog() throws Exception {
        InputStream in = getClass().getResourceAsStream("log.txt");
        new BuildStateReader().parseBuildState(new InputStreamReader(in));
    }

    @Test
    public void testParseBuildLogWithParams() throws Exception {
        InputStream in = getClass().getResourceAsStream("log-param.txt");
        new BuildStateReader().parseBuildState(new InputStreamReader(in));
    }

}
