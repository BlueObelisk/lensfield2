/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import java.io.IOException;
import java.io.Reader;

/**
 * @author sea36
 */
public class LensfieldTokeniser {

    private static final Token ARRAY_OPEN = new Token(TokenType.ARRAY_OPEN);
    private static final Token ARRAY_CLOSE = new Token(TokenType.ARRAY_CLOSE);
    private static final Token GROUP_OPEN = new Token(TokenType.GROUP_OPEN);
    private static final Token GROUP_CLOSE = new Token(TokenType.GROUP_CLOSE);
    private static final Token EOF = new Token(TokenType.EOF);

    enum TokenType {
        LITERAL,
        ARRAY_OPEN,
        ARRAY_CLOSE,
        GROUP_OPEN,
        GROUP_CLOSE,
        EOF
    }


    private Reader in;
    private int ch;


    public LensfieldTokeniser(Reader in) throws IOException {
        this.in = in;
        ch = in.read();
    }


    public synchronized Token nextToken() throws IOException {
        skipWhitespaceAndComments();

        if (isEOF(ch)) {
            return EOF;
        }

        switch (ch) {
            case '(':
                ch = in.read();
                return GROUP_OPEN;
            case ')':
                ch = in.read();
                return GROUP_CLOSE;
            case '[':
                ch = in.read();
                return ARRAY_OPEN;
            case ']':
                ch = in.read();
                return ARRAY_CLOSE;
            case '"':
            case '\'':
                return readQuotedLiteral();
        }
        return readUnquotedLiteral();
    }

    private Token readQuotedLiteral() throws IOException {
        int quot = ch;
        StringBuilder s = new StringBuilder();
        for (ch = in.read(); ch != quot; ch = in.read()) {
            if (isEOL(ch) || isEOF(ch)) {
                throw new IOException("Unterminated quoted string");
            }
            s.append((char)ch);
        }
        ch = in.read();
        return new Token(s.toString());
    }

    private Token readUnquotedLiteral() throws IOException {
        StringBuilder s = new StringBuilder();
        while (!(isWhitespace(ch) || isSpecial(ch) || isEOF(ch))) {
            s.append((char)ch);
            ch = in.read();
        }
        return new Token(s.toString());
    }

    private boolean isSpecial(int ch) {
        return ch == '(' || ch == ')' || ch == '[' || ch == ']';
    }

    private static boolean isWhitespace(int ch) {
        return Character.isWhitespace((char)ch);
    }

    private static boolean isEOF(int ch) {
        return ch == -1;
    }

    private static boolean isEOL(int ch) {
        return ch == '\n' || ch == '\r';
    }

    private void skipWhitespaceAndComments() throws IOException {
        while (Character.isWhitespace(ch) || ch == ';') {
            if (ch == ';') {
                skipComment();
            } else {
                ch = in.read();
            }
        }
    }

    private void skipComment() throws IOException {
        // Skip from the current character to EOL/EOF
        while (!(isEOL(ch) || isEOF(ch))) {
            ch = in.read();
        }
    }


    public static class Token {

        final TokenType type;
        final String value;

        Token(String value) {
            this.type = TokenType.LITERAL;
            this.value = value;
        }

        Token(TokenType type) {
            this.type = type;
            this.value = null;
        }

        @Override
        public String toString() {
            return type == TokenType.LITERAL ? type.name()+'['+value+']' : type.name();
        }
    }


}
