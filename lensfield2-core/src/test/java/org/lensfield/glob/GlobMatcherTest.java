package org.lensfield.glob;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author sea36
 */
public class GlobMatcherTest {

    private static File workspace;

    @BeforeClass
    public static void setup() throws IOException {
        System.err.println("[*TEST*] Creating workspace");
        workspace = new File("target/temp", UUID.randomUUID().toString());
        if (!workspace.mkdirs()) {
            throw new IOException("Failed to create workspace: "+workspace);
        }
        File input = new File("src/test/data/files");
        FileUtils.copyDirectory(input, workspace);
    }

    @AfterClass
    public static void cleanup() throws IOException {
        System.err.println("[*TEST*] Cleaning workspace");
        FileUtils.deleteDirectory(workspace);
        System.err.println();
    }


    @Test
    public void testFile() {
        GlobMatcher glob = new GlobMatcher("a.a");
        assertMatch(glob.find(workspace), "a.a");
    }

    @Test
    public void testDirFile() {
        GlobMatcher glob = new GlobMatcher("a/a.b");
        assertMatch(glob.find(workspace), "a/a.b");
    }

    @Test
    public void testWildcardFile() {
        GlobMatcher glob = new GlobMatcher("*");
        assertMatch(glob.find(workspace), "a.a", "a.b", "a.c", "b.a");
    }

    @Test
    public void testWildcardFilePrefix() {
        GlobMatcher glob = new GlobMatcher("a.*");
        assertMatch(glob.find(workspace), "a.a", "a.b", "a.c");
    }

    @Test
    public void testWildcardFileSuffix() {
        GlobMatcher glob = new GlobMatcher("*.a");
        assertMatch(glob.find(workspace), "a.a", "b.a");
    }

    @Test
    public void testWildcardDir() {
        GlobMatcher glob = new GlobMatcher("*/a.a");
        assertMatch(glob.find(workspace), "a/a.a", "b/a.a", "c/a.a");
    }

    @Test
    public void testDirWildcardDir() {
        GlobMatcher glob = new GlobMatcher("a/*/a.a");
        assertMatch(glob.find(workspace), "a/b/a.a", "a/c/a.a");
    }

    @Test
    public void testWildcardDirDir() {
        GlobMatcher glob = new GlobMatcher("*/b/a.a");
        assertMatch(glob.find(workspace), "a/b/a.a", "b/b/a.a");
    }

    @Test
    public void testWildcardFnPrefix() {
        GlobMatcher glob = new GlobMatcher("c/de*");
        assertMatch(glob.find(workspace), "c/defgh", "c/dexyz");
    }

    @Test
    public void testWildcardFnSuffix() {
        GlobMatcher glob = new GlobMatcher("c/*h");
        assertMatch(glob.find(workspace), "c/defgh", "c/dabch");
    }

    @Test
    public void testWildcardFnMiddle() {
        GlobMatcher glob = new GlobMatcher("c/d*h");
        assertMatch(glob.find(workspace), "c/defgh", "c/dabch");
    }

    @Test
    public void testWilddirFile() {
        GlobMatcher glob = new GlobMatcher("**/a.a");
        assertMatch(glob.find(workspace), "a.a", "a/a.a", "a/b/a.a", "a/b/c/a.a", "a/c/a.a", "b/a.a",
                "b/b/a.a", "b/b/c/a.a", "c/a.a");
    }

    @Test
    public void testWilddirDirFile() {
        GlobMatcher glob = new GlobMatcher("**/c/a.a");
        assertMatch(glob.find(workspace), "a/b/c/a.a", "a/c/a.a", "b/b/c/a.a", "c/a.a");
    }

    @Test
    public void testDirWilddirFile() {
        GlobMatcher glob = new GlobMatcher("a/**/a.a");
        assertMatch(glob.find(workspace), "a/a.a", "a/b/a.a", "a/b/c/a.a", "a/c/a.a");
    }

    private void assertMatch(List<String> list, String... files) {
        Set<String> set = new HashSet<String>(list);
        for (String f : files) {
            assertTrue(f, set.contains(f));
        }
        assertEquals("count", files.length, set.size());
    }

}
