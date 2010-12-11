package org.lensfield.log;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

/**
 * @author Sam Adams
 */
public class LensfieldWriterTest {

    public static final String writeToken(String s) {
        StringWriter buffer = new StringWriter();
        PrintWriter out = new PrintWriter(buffer);
        LensfieldWriter lfw = new LensfieldWriter(out);
        try {
            lfw.writeToken(s);
        } finally {
            lfw.close();
        }
        return buffer.toString();
    }

    @Test
    public void testWriteEmptyToken() {
        String s = writeToken("");
        assertEquals("''", s);
    }

    @Test
    public void testWriteSimpleToken() {
        String s = writeToken("foo");
        assertEquals("foo", s);
    }

    @Test
    public void testWriteSpaceToken() {
        String s = writeToken(" ");
        assertEquals("' '", s);
    }

    @Test
    public void testWriteTabToken() {
        String s = writeToken("\t");
        assertEquals("'\t'", s);
    }

    @Test
    public void testWriteTokenWithSpace() {
        String s = writeToken("foo bar");
        assertEquals("'foo bar'", s);
    }

    @Test
    public void testWriteTokenWithSingleQuote() {
        String s = writeToken("foo'bar");
        assertEquals("\"foo'bar\"",s);
    }

    @Test
    public void testWriteTokenWithDoubleQuote() {
        String s = writeToken("foo\"bar");
        assertEquals("'foo\"bar'",s);
    }

    @Test
    public void testWriteTokenWithBothQuotes() {
        String s = writeToken("foo'\"bar");
        assertEquals("'foo\\'\"bar'",s);
    }

}
