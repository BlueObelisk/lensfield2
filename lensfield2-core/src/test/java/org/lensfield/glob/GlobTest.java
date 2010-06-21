package org.lensfield.glob;

import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author sea36
 */
public class GlobTest {

    @Test
    public void testFormat() throws MissingParameterException {
        Glob glob = new Glob("a/**/b/*.x");
        Map<String,String> params = new HashMap<String, String>();
        params.put("**", "foo/bar");
        params.put("*", "xyzzy");
        assertEquals("a/foo/bar/b/xyzzy.x", glob.format(params));
    }

    @Test
    public void testFormatWithEmptyParam() throws MissingParameterException {
        Glob glob = new Glob("a/**/b/*.x");
        Map<String,String> params = new HashMap<String, String>();
        params.put("**", "");
        params.put("*", "xyzzy");
        assertEquals("a/b/xyzzy.x", glob.format(params));
    }

    @Test(expected = MissingParameterException.class)
    public void testFormatWithMissingParam() throws MissingParameterException {
        Glob glob = new Glob("a/**/b/*.x");
        Map<String,String> params = new HashMap<String, String>();
        params.put("**", "");
        glob.format(params);
    }

    @Test(expected = MissingParameterException.class)
    public void testFormatWithNullParam() throws MissingParameterException {
        Glob glob = new Glob("a/**/b/*.x");
        Map<String,String> params = new HashMap<String, String>();
        params.put("**", "");
        params.put("*", null);
        glob.format(params);
    }

}
