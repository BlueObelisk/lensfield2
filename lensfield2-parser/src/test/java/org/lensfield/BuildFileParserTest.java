/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author sea36
 */
public class BuildFileParserTest {

    @Test
    public void testParseExampleBuildFile() throws Exception {
        InputStream in = BuildFileParserTest.class.getResourceAsStream("build.lf");
        if (in == null) {
            throw new FileNotFoundException();
        }
        new BuildFileParser().parse(new InputStreamReader(in, "UTF-8"));
    }

    @Test
    public void testParseBuildFileStartsWithComment() throws Exception {
        InputStream in = BuildFileParserTest.class.getResourceAsStream("build-comment.lf");
        if (in == null) {
            throw new FileNotFoundException();
        }
        new BuildFileParser().parse(new InputStreamReader(in, "UTF-8"));
    }

}
