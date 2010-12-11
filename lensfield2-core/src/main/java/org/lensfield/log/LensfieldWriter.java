package org.lensfield.log;

import java.io.PrintWriter;

/**
 * @author Sam Adams
 */
public class LensfieldWriter {

    private PrintWriter out;

    public LensfieldWriter(PrintWriter out) {
        if (out == null) {
            throw new IllegalArgumentException("Null argument");
        }
        this.out = out;
    }

    public void writeToken(String s) {
        if (s.length() == 0) {
            out.print("''");
        }
        else if (needsQuoting(s)) {
            writeQuotedToken(s);
        } else {
            writeUnquotedToken(s);
        }
    }

    private void writeUnquotedToken(String s) {
        out.print(s);
    }

    private void writeQuotedToken(String s) {
        if (s.indexOf('\'') == -1) {
            writeQuotedToken('\'', s);
        }
        else if (s.indexOf('"') == -1) {
            writeQuotedToken('"', s);
        }
        else {
            writeQuotedToken('\'', s.replace("\'", "\\\'"));
        }
    }

    private void writeQuotedToken(char c, String s) {
        out.print(c);
        out.print(s);
        out.print(c);
    }

    private boolean needsQuoting(String s) {
        if (containsWhitespace(s)) {
            return true;
        }
        return containsSpecialCharacter(s);
    }

    private boolean containsSpecialCharacter(String s) {
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\'' || c == '(' || c == ')' || c == '[' || c == ']') {
                return true;
            }
        }
        return false;
    }

    private boolean containsWhitespace(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
    }


    public void writeList(String... list) {
        out.print('[');
        for (int i = 0; i < list.length; i++) {
            if (i != 0) {
                out.print(' ');
            }
            writeToken(list[i]);
        }
        out.print(']');
    }

    public void close() {
        out.close();
    }
    
}
