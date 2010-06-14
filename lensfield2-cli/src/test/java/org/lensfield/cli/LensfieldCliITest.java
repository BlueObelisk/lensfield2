/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.cli;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author sea36
 */
public class LensfieldCliITest {

private File workspace;

    @Before
    public void setup() throws IOException {
        System.err.println("[*TEST*] Creating workspace");
        workspace = new File("target/temp", UUID.randomUUID().toString());
        if (!workspace.mkdirs()) {
            throw new IOException("Failed to create workspace: "+workspace);
        }
    }

    @After
    public void cleanup() throws IOException {
        System.err.println("[*TEST*] Cleaning workspace");
//        FileUtils.deleteDirectory(workspace);
        System.err.println();
    }


    public static void loadNumberData(File workspace) throws IOException {
        File input = new File("src/test/data/numbers");
        FileUtils.copyDirectory(input, workspace);
    }

    public static void loadMoleculeData(File workspace) throws IOException {
        File input = new File("src/test/data/molecules");
        FileUtils.copyDirectory(input, workspace);
    }


    @Test
    public void testNumbers() throws Exception {
        loadNumberData(workspace);
        LensfieldCli.main(new String[]{workspace.getPath()});

        assertEquals("285", FileUtils.readFileToString(new File(workspace, "sum-sq.txt")));
        assertEquals("90", FileUtils.readFileToString(new File(workspace, "sum-x2.txt")));        
    }


    @Test
    public void testMolecules() throws Exception {
        loadMoleculeData(workspace);
        LensfieldCli.main(new String[]{workspace.getPath()});

        assertTrue(new File(workspace, "cyclobutadiene.cml").length() > 0);
        assertTrue(new File(workspace, "phenol.cml").length() > 0);
        assertTrue(new File(workspace, "propanone.cml").length() > 0);
    }


    @Test
    public void testBuildStepDependencies() throws Exception {
        loadNumberData(workspace);
        LensfieldCli.main(new String[]{workspace.getPath()+"/build-depend.lf"});

        assertEquals("54", FileUtils.readFileToString(new File(workspace, "sum1.txt")));
        assertEquals("63", FileUtils.readFileToString(new File(workspace, "sum2.txt")));
    }
}
