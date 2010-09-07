package org.lensfield;

import org.junit.Test;
import org.lensfield.LensfieldTokeniser.Token;
import org.lensfield.LensfieldTokeniser.TokenType;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

/**
 * @author sea36
 */
public class LensfieldTokeniserTest {

    private LensfieldTokeniser open(String s) throws IOException {
        return new LensfieldTokeniser(new StringReader(s));
    }

    @Test
    public void testGroupOpen() throws IOException {
        LensfieldTokeniser tok = open("(");
        assertEquals(TokenType.GROUP_OPEN, tok.nextToken().type);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }

    @Test
    public void testGroupClose() throws IOException {
        LensfieldTokeniser tok = open(")");
        assertEquals(TokenType.GROUP_CLOSE, tok.nextToken().type);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }

    @Test
    public void testArrayOpen() throws IOException {
        LensfieldTokeniser tok = open("[");
        assertEquals(TokenType.ARRAY_OPEN, tok.nextToken().type);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }

    @Test
    public void testArrayClose() throws IOException {
        LensfieldTokeniser tok = open("]");
        assertEquals(TokenType.ARRAY_CLOSE, tok.nextToken().type);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }

    @Test
    public void testUnquotedLiteral() throws IOException {
        LensfieldTokeniser tok = open("foo");
        Token token = tok.nextToken();
        assertEquals(TokenType.LITERAL, token.type);
        assertEquals("foo", token.value);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }

    @Test
    public void testSingleQuotedLiteral() throws IOException {
        LensfieldTokeniser tok = open("'foo'");
        Token token = tok.nextToken();
        assertEquals(TokenType.LITERAL, token.type);
        assertEquals("foo", token.value);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }

    @Test
    public void testDoubleQuotedLiteral() throws IOException {
        LensfieldTokeniser tok = open("\"foo\"");
        Token token = tok.nextToken();
        assertEquals(TokenType.LITERAL, token.type);
        assertEquals("foo", token.value);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }

    @Test
    public void testLiteralWithWhitespacePrefix() throws IOException {
        LensfieldTokeniser tok = open("   foo");
        Token token = tok.nextToken();
        assertEquals(TokenType.LITERAL, token.type);
        assertEquals("foo", token.value);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }

    @Test
    public void testLiteralWithWhitespaceSuffix() throws IOException {
        LensfieldTokeniser tok = open("foo   ");
        Token token = tok.nextToken();
        assertEquals(TokenType.LITERAL, token.type);
        assertEquals("foo", token.value);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }


    @Test
    public void testLiteralWithCommentPrefix() throws IOException {
        LensfieldTokeniser tok = open("; abc\nfoo");
        Token token = tok.nextToken();
        assertEquals(TokenType.LITERAL, token.type);
        assertEquals("foo", token.value);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }

    @Test
    public void testLiteralWithCommentSuffix() throws IOException {
        LensfieldTokeniser tok = open("foo ; xyz");
        Token token = tok.nextToken();
        assertEquals(TokenType.LITERAL, token.type);
        assertEquals("foo", token.value);
        assertEquals(TokenType.EOF, tok.nextToken().type);
    }

    @Test(expected=IOException.class)
    public void testUnterminatedDoubleQuotedLiteral() throws IOException {
        LensfieldTokeniser tok = open("\"foo'");
        tok.nextToken();
    }

    @Test(expected=IOException.class)
    public void testUnterminatedSingleQuotedLiteral() throws IOException {
        LensfieldTokeniser tok = open("'foo");
        tok.nextToken();
    }

}
