/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.lensfield.model.BuildStep;
import org.lensfield.model.Model;
import org.lensfield.model.Source;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author sea36
 */
public class LensfieldProcessorITest {

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
        FileUtils.deleteDirectory(workspace);
        System.err.println();
    }


    public static void loadNumberData(File workspace) throws IOException {
        File input = new File("src/test/data/numbers");
        FileUtils.copyDirectory(input, workspace);
    }

    public static void loadAbcData(File workspace) throws IOException {
        File input = new File("src/test/data/abc");
        FileUtils.copyDirectory(input, workspace);
    }


    public static void loadXyzData(File workspace) throws IOException {
        File input = new File("src/test/data/xyz");
        FileUtils.copyDirectory(input, workspace);
    }


    public static void loadMoleculeData(File workspace) throws IOException {
        File input = new File("src/test/data/molecules");
        FileUtils.copyDirectory(input, workspace);
    }



    public static void genBigData(File workspace, int n0, int n1, int n2) throws IOException {
        System.err.print("[*TEST*] Generating data ");
        for (int i0 = 1; i0 <= n0; i0++) {
            System.err.print(". ");
            File f0 = new File(workspace, Integer.toString(i0));
            f0.mkdir();
            for (int i1 = 1; i1 <= n1; i1++) {
                File f1 = new File(f0, Integer.toString(i1));
                f1.mkdir();
                for (int i2 = 1; i2 <= n2; i2++) {
                    File f2 = new File(f1, i2+".x");
                    FileUtils.writeStringToFile(f2, Integer.toString(i2));
                }
            }
        }
        System.err.println();
    }


    @Test
    public void testNumbers() throws Exception {
        loadNumberData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addSource(new Source("files", "**/*.n"));
        model.addBuildStep(new BuildStep("doubles",     "org.lensfield.testing.ops.number.Doubler",  "files",    "**/*.x2"));
        model.addBuildStep(new BuildStep("squares",     "org.lensfield.testing.ops.number.Squarer",  "files",    "**/*.sq"));
        model.addBuildStep(new BuildStep("copies",      "org.lensfield.testing.ops.file.Copier",     "doubles",  "**/*.nn"));
        model.addBuildStep(new BuildStep("sum-squares", "org.lensfield.testing.ops.number.Summer",   "squares",  "sum-sq.txt"));
        model.addBuildStep(new BuildStep("sum-doubles", "org.lensfield.testing.ops.number.Summer",   "doubles",  "sum-x2.txt"));

        Lensfield lensfield = new Lensfield(model, workspace);
        lensfield.build();

        assertEquals("285", FileUtils.readFileToString(new File(workspace, "sum-sq.txt")));
        assertEquals("90", FileUtils.readFileToString(new File(workspace, "sum-x2.txt")));

    }


    @Test
    public void testAbcCopy() throws Exception {
        loadAbcData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addSource(new Source("files", "input/*"));
        model.addBuildStep(new BuildStep("copies", "org.lensfield.testing.ops.file.Copier", "files", "output/*"));

        Lensfield lf = new Lensfield(model, workspace);
        lf.build();

        assertTrue(new File(workspace, "output/a.txt").isFile());
        assertTrue(new File(workspace, "output/b.txt").isFile());

    }

    @Test
    public void testAbcJoinN1() throws Exception {
        loadAbcData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addSource(new Source("files", "input/*"));
        model.addBuildStep(new BuildStep("joined", "org.lensfield.testing.ops.file.Joiner", "files", "output/all.txt"));

        Lensfield lf = new Lensfield(model, workspace);
        lf.build();

        File f = new File(workspace, "output/all.txt");
        String s = FileUtils.readFileToString(f);
        assertEquals("aaaaabbbbb", s);

    }


    @Test
    public void testChain11() throws Exception {
        loadAbcData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addSource(new Source("files", "input/*"));
        model.addBuildStep(new BuildStep("copy1", "org.lensfield.testing.ops.file.Copier", "files", "output/1-*"));
        model.addBuildStep(new BuildStep("copy2", "org.lensfield.testing.ops.file.Copier", "copy1", "output/2-*"));
        model.addBuildStep(new BuildStep("copy3", "org.lensfield.testing.ops.file.Copier", "copy2", "output/3-*"));

        Lensfield lf = new Lensfield(model, workspace);
        lf.build();

        assertTrue(new File(workspace, "output/1-a.txt").isFile());
        assertTrue(new File(workspace, "output/1-b.txt").isFile());
        assertTrue(new File(workspace, "output/2-a.txt").isFile());
        assertTrue(new File(workspace, "output/2-b.txt").isFile());
        assertTrue(new File(workspace, "output/3-a.txt").isFile());
        assertTrue(new File(workspace, "output/3-b.txt").isFile());

    }

    @Test
    public void testChain11Repeat() throws Exception {
        loadAbcData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addSource(new Source("files", "input/*"));
        model.addBuildStep(new BuildStep("copy1", "org.lensfield.testing.ops.file.Copier", "files", "output/1-*"));
        model.addBuildStep(new BuildStep("copy2", "org.lensfield.testing.ops.file.Copier", "copy1", "output/2-*"));
        model.addBuildStep(new BuildStep("copy3", "org.lensfield.testing.ops.file.Copier", "copy2", "output/3-*"));

        Lensfield lf = new Lensfield(model, workspace);
        lf.build();

        assertTrue(new File(workspace, "output/1-a.txt").isFile());
        assertTrue(new File(workspace, "output/1-b.txt").isFile());
        assertTrue(new File(workspace, "output/2-a.txt").isFile());
        assertTrue(new File(workspace, "output/2-b.txt").isFile());
        assertTrue(new File(workspace, "output/3-a.txt").isFile());
        assertTrue(new File(workspace, "output/3-b.txt").isFile());

        Map<File,Long> ages = getAges(new File(workspace, "output/"));
        Thread.sleep(1000);

        new Lensfield(model, workspace).build();
        checkAges(new File(workspace, "output/"), ages);
    }

    private static void checkAges(File file, Map<File, Long> ages) {
        for (File f : file.listFiles()) {
            if (!ages.containsKey(f)) {
                fail("File present: "+f);
            }
            if (f.lastModified() != ages.get(f).longValue()) {
                fail("File not modified: "+f);
            }
        }
    }

    private static Map<File, Long> getAges(File file) {
        Map<File,Long> map = new HashMap<File, Long>();
        for (File f : file.listFiles()) {
            map.put(f, f.lastModified());
        }
        return map;
    }


    @Test
    public void testXyzSplit1N() throws Exception {
        loadXyzData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addSource(new Source("files", "input/*.txt"));
        model.addBuildStep(new BuildStep("split", "org.lensfield.testing.ops.file.Splitter", "files", "output/*-{%i}.txt"));

        Lensfield lf = new Lensfield(model, workspace);
        lf.build();

        assertEquals("11111", FileUtils.readFileToString(new File(workspace, "output/x-1.txt")));
        assertEquals("22222", FileUtils.readFileToString(new File(workspace, "output/x-2.txt")));
        assertEquals("33333", FileUtils.readFileToString(new File(workspace, "output/x-3.txt")));
        assertEquals("aaaaa", FileUtils.readFileToString(new File(workspace, "output/y-1.txt")));
        assertEquals("bbbbb", FileUtils.readFileToString(new File(workspace, "output/y-2.txt")));
        assertEquals("ccccc", FileUtils.readFileToString(new File(workspace, "output/y-3.txt")));
    }

    @Test
    public void testXyzChain() throws Exception {
        loadXyzData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addSource(new Source("files", "input/*.txt"));
        model.addBuildStep(new BuildStep("split", "org.lensfield.testing.ops.file.Splitter", "files", "output/*-{%i}.txt"));
        model.addBuildStep(new BuildStep("copy", "org.lensfield.testing.ops.file.Copier", "split", "output/*-{%i}-copy.txt"));
        model.addBuildStep(new BuildStep("join", "org.lensfield.testing.ops.file.Joiner", "copy", "output/*-join.txt"));

        Lensfield lf = new Lensfield(model, workspace);
        lf.build();

        assertEquals("111112222233333", FileUtils.readFileToString(new File(workspace, "output/x-join.txt")));
        assertEquals("aaaaabbbbbccccc", FileUtils.readFileToString(new File(workspace, "output/y-join.txt")));

    }


    @Test
    public void testSum() throws Exception {
        loadNumberData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addSource(new Source("files", "**/*.n"));
        BuildStep build1 = new BuildStep("sum1", "org.lensfield.testing.ops.number.Summer",   "files",  "sum1.txt");
        build1.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addBuildStep(build1);
        BuildStep build2 = new BuildStep("sum2", "org.lensfield.testing.ops.number.Summer",   "files",  "sum2.txt");
        build2.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addBuildStep(build2);

        Lensfield lf = new Lensfield(model, workspace);
        lf.build();

        assertEquals("45", FileUtils.readFileToString(new File(workspace, "sum1.txt")));
        assertEquals("45", FileUtils.readFileToString(new File(workspace, "sum2.txt")));
    }


    @Test
    public void testBuildStepDependencies() throws Exception {
        loadNumberData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");

        model.addSource(new Source("files", "**/*.n"));
        BuildStep inc1 = new BuildStep("inc1", "org.lensfield.testing.ops.Incer",   "files",  "**/*.1");
        inc1.addDependency("org.lensfield.testing", "lensfield2-testops2", "0.2");
        model.addBuildStep(inc1);
        BuildStep inc2 = new BuildStep("inc2", "org.lensfield.testing.ops.Incer",   "files",  "**/*.2");
        inc2.addDependency("org.lensfield.testing", "lensfield2-testops3", "0.2");
        model.addBuildStep(inc2);

        model.addBuildStep(new BuildStep("sum1", "org.lensfield.testing.ops.number.Summer",   "inc1",  "sum1.txt"));
        model.addBuildStep(new BuildStep("sum2", "org.lensfield.testing.ops.number.Summer",   "inc2",  "sum2.txt"));

        Lensfield lf = new Lensfield(model, workspace);
        lf.build();

        assertEquals("54", FileUtils.readFileToString(new File(workspace, "sum1.txt")));
        assertEquals("63", FileUtils.readFileToString(new File(workspace, "sum2.txt")));
    }


    @Test
    public void testRepeatKLUnchanged() throws Exception {
        loadAbcData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addSource(new Source("files", "input/*"));
        model.addBuildStep(new BuildStep("copies", "org.lensfield.testing.ops.file.Copier", "files", "output/*"));

        new Lensfield(model, workspace).build();
        assertTrue(new File(workspace, "output/a.txt").isFile());
        assertTrue(new File(workspace, "output/b.txt").isFile());
        long a = new File(workspace, "output/a.txt").lastModified();
        long b = new File(workspace, "output/b.txt").lastModified();

        Thread.sleep(1000);
        new Lensfield(model, workspace).build();
        assertEquals("file not modified", a, new File(workspace, "output/a.txt").lastModified());
        assertEquals("file not modified", b, new File(workspace, "output/b.txt").lastModified());
    }

    @Test
    public void testRepeatKLUpdated() throws Exception {
        loadAbcData(workspace);

        Model model = new Model();
        model.addRepository("https://maven.ch.cam.ac.uk/m2repo");
        model.addDependency("org.lensfield.testing", "lensfield2-testops1", "0.2");
        model.addSource(new Source("files", "input/*"));
        model.addBuildStep(new BuildStep("copies", "org.lensfield.testing.ops.file.Copier", "files", "output/*"));

        new Lensfield(model, workspace).build();
        assertTrue(new File(workspace, "output/a.txt").isFile());
        assertTrue(new File(workspace, "output/b.txt").isFile());
        long a = new File(workspace, "output/a.txt").lastModified();
        long b = new File(workspace, "output/b.txt").lastModified();

        FileUtils.writeStringToFile(new File(workspace, "input/a.txt"), "foo");

        Thread.sleep(1000);
        new Lensfield(model, workspace).build();
        assertFalse("file modified", a == new File(workspace, "output/a.txt").lastModified());
        assertEquals("file not modified", b, new File(workspace, "output/b.txt").lastModified());
    }


    @Test
    @Ignore
    public void testBigData() throws Exception {
        genBigData(workspace, 10, 10, 100);
//        Lensfield lensfield = new Lensfield();
//        lensfield.setRoot(workspace);
//        lensfield.addDependency("org.lensfield.testing", "lensfield2-testops", "0.1-SNAPSHOT");
//
//        lensfield.addBuildStep(new BuildStep("files",       "org.lensfield.ops.FileFinder",      null,       "filter", "**/*.x", "root", workspace.getPath()));
//        lensfield.addBuildStep(new BuildStep("doubles",     "org.lensfield.testing.ops.number.Doubler",  "files",    "**/*.x2"));
//        lensfield.addBuildStep(new BuildStep("sum",         "org.lensfield.testing.ops.number.Summer",   "doubles",  "sum.txt"));
//
//        lensfield.build();
//
        assertEquals("1010000", FileUtils.readFileToString(new File(workspace, "sum.txt")));

    }


}
