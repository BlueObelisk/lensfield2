/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.model.*;
import org.lensfield.model.Process;

import java.io.*;

/**
 * @author sea36
 */
public class BuildFileParser {

    private Model model;

    private Reader in;
    private char ch;

    public synchronized Model parse(Reader in) throws IOException {
        this.model = new Model();
        this.in = in;
        this.ch = (char) in.read();
        parse();
        Model model = this.model;
        this.model = null;
        this.in = null;
        this.ch = (char) -1;
        return model;
    }

    public Model parse(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        try {
            return parse(in);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void parse() throws IOException {
        skipWhitespaceAndComments();
        if (isEof()) {
            return;
        }
        while (!isEof()) {
            if ('(' == ch) {
                ch = (char) in.read();
                String command = readToken();
                if ("depends".equals(command)) {
                    parseDepends();
                }
                else if ("source".equals(command)) {
                    parseSource(null);
                }
                else if (command.startsWith("source/")) {
                    parseSource(command.substring(7));
                }
                else if ("build".equals(command)) {
                    parseBuild();
                }
                else if ("repository".equals(command)) {
                    parseRepository();
                }
                else {
                    throw new IOException("Unknown command: "+command);
                }
                skipWhitespaceAndComments();
                if (')' != ch) {
                    throw new IOException("Expected ')'");
                }
                ch = (char) in.read();
            }
            skipWhitespaceAndComments();
        }
    }

    private void parseRepository() throws IOException {
        skipWhitespaceAndComments();
        while (ch != ')') {
            if (isEof()) {
                throw new EOFException();
            }
            String repository = readToken();
            model.addRepository(repository);
            skipWhitespaceAndComments();
        }
    }

    private void parseDepends() throws IOException {
        skipWhitespaceAndComments();
        while (ch != ')') {
            String dependency = readToken();
            int i0 = dependency.indexOf(':');
            int i1 = dependency.indexOf(':', i0+1);
            if (i0 == -1 || i1 == -1) {
                    throw new IOException("Bad dependency; format groupId:artifactId:version");
                }
            String groupId = dependency.substring(0, i0);
            String artifactId = dependency.substring(i0+1, i1);
            String version = dependency.substring(i1+1);
            model.addDependency(groupId, artifactId, version);
            skipWhitespaceAndComments();
        }
    }

    private void parseSource(String className) throws IOException {
        skipWhitespaceAndComments();
        if (')' == ch) {
            throw new IOException("Expected source name");
        }
        String name = readToken();
        skipWhitespaceAndComments();
        if (')' == ch) {
            throw new IOException("Expected source glob");
        }
        String glob = readToken();
        Source source = new Source(name, glob);
        if (className != null) {
            source.setClassname(className);
        }
        parseSourceParameters(source);
        model.addSource(source);
    }

    private void parseSourceParameters(Source source) throws IOException {
        skipWhitespaceAndComments();
        while (')' != ch) {
            if (isEof()) {
                throw new EOFException();
            }
            String arg = readToken();
            if (":param".equals(arg) || ":parameter".equals(arg)) {
                parseParameter(source);
            }
            else if (":depends".equals(arg)) {
                parseDepends(source);
            }
            else {
                throw new IOException("Expected source parameter; found '"+arg+"'");
            }
            skipWhitespaceAndComments();
        }
    }

    private void parseBuild() throws IOException {
        skipWhitespaceAndComments();
        if (isEof()) {
            throw new EOFException();
        }
        if (')' == ch) {
            throw new IOException("Expected build name");
        }
        String name = readToken();
        skipWhitespaceAndComments();
        if (')' == ch) {
            throw new IOException("Expected build class");
        }
        String clazz = readToken();
        Build build = new Build(name, clazz);
        parseBuildArgs(build);
        model.addBuild(build);
    }

    private void parseBuildArgs(Build build) throws IOException {
        skipWhitespaceAndComments();
        while (')' != ch) {
            if (isEof()) {
                throw new EOFException();
            }
            String arg = readToken();
            if (":input".equals(arg)) {
                parseInput(build);
            }
            else if (":output".equals(arg)) {
                parseOutput(build);
            }
            else if (":param".equals(arg) || ":parameter".equals(arg)) {
                parseParameter(build);
            }
            else if (":depends".equals(arg)) {
                parseDepends(build);
            }
            else {
                throw new IOException("Expected build argument; found '"+arg+"'");
            }
            skipWhitespaceAndComments();                        
        }
    }

    private void parseInput(Build build) throws IOException {
        skipWhitespaceAndComments();
        String x = readToken();
        skipWhitespaceAndComments();
        if (isEof()) {
            throw new EOFException();
        }
        if (ch == ':' || ch == ')') {
            build.addInput(new Input(x));
        } else {
            String y = readToken();
            build.addInput(new Input(x, y));
        }
    }

    private void parseOutput(Build build) throws IOException {
        skipWhitespaceAndComments();
        String x = readToken();
        skipWhitespaceAndComments();
        if (isEof()) {
            throw new EOFException();
        }
        if (ch == ':' || ch == ')') {
            build.addOutput(new Output(x));
        } else {
            String y = readToken();
            build.addOutput(new Output(x, y));
        }
    }

    private void parseParameter(Process build) throws IOException {
        skipWhitespaceAndComments();
        String x = readToken();
        skipWhitespaceAndComments();
        if (isEof()) {
            throw new EOFException();
        }
        if (ch == ':' || ch == ')') {
            build.addParameter(new Parameter(x));
        } else {
            String y = readToken();
            build.addParameter(new Parameter(x, y));
        }
    }

    private void parseDepends(Process build) throws IOException {
        skipWhitespaceAndComments();
        if (isEof()) {
            throw new EOFException();
        }
        String d = readToken();
        int i0 = d.indexOf(':');
        int i1 = d.indexOf(':', i0+1);
        if (i0 == -1 || i1 == -1) {
            throw new IOException("Bad dependency. Expected groupId:artifactId:version; found "+d);
        }
        build.addDependency(d.substring(0,i0), d.substring(i0+1,i1), d.substring(i1+1));
    }


    private String readToken() throws IOException {
        if ('"' == ch || '\'' == ch) {
            return readQuotedToken();
        }
        return readUnquotedToken();
    }

    private String readUnquotedToken() throws IOException {
        StringBuilder s = new StringBuilder();
        while (!Character.isWhitespace(ch) && ')' != ch && ch != ';') {
            if (isEof()) {
                throw new EOFException("Unexpected EOF");
            }
            s.append(ch);
            ch = (char) in.read();
        }
        return s.toString();
    }

    private String readQuotedToken() throws IOException {
        char quot = ch;
        ch = (char) in.read();
        StringBuilder s = new StringBuilder();
        while (quot != ch) {
            if (isEof()) {
                throw new EOFException("Unexpected EOF");
            }
            if ('\r' == ch || '\n' == ch) {
                throw new IOException("Unexpected line-break");
            }
            if ('\\' == ch) {
                // TODO handle escaping
            }
            s.append(ch);
            ch = (char) in.read();
        }
        ch = (char) in.read();
        return s.toString();
    }

    private void skipWhitespaceAndComments() throws IOException {
        while (Character.isWhitespace(ch) || ';' == ch) {
            ch = (char) in.read();
            if (';' == ch) {
                skipComment();
            }
        }
    }

    private void skipComment() throws IOException {
        while (ch != '\r' && ch != '\n' && !isEof()) {
            ch = (char) in.read();
        }
    }

    private boolean isEof() {
        return ch == (char)-1;
    }

}
