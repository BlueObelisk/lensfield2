/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.lensfield.LensfieldTokeniser.TokenType;
import org.lensfield.model.Build;
import org.lensfield.model.Input;
import org.lensfield.model.Model;
import org.lensfield.model.Output;
import org.lensfield.model.Parameter;
import org.lensfield.model.Process;
import org.lensfield.model.Source;

import java.io.IOException;
import java.io.Reader;

/**
 * @author sea36
 */
public class BuildFileReader {

    private LensfieldTokeniser tokeniser;
    private LensfieldTokeniser.Token token;

    private Model model;

    public synchronized Model parse(Reader in) throws IOException {
        tokeniser = new LensfieldTokeniser(in);
        try {
            model = new Model();
            parse();
            return model;
        } finally {
            this.model = null;
            this.tokeniser = null;
            in.close();
        }
    }

    private void parse() throws IOException {
        while (readToken().type != TokenType.EOF) {
            if (token.type != LensfieldTokeniser.TokenType.GROUP_OPEN) {
                throw new IOException("Expected: GROUP_OPEN; found: "+token);
            }
            readLiteral();
            if ("repository".equals(token.value)) {
                parseRepository();
            }
            else if ("depends".equals(token.value)) {
                parseDepends();
            }
            else if ("source".equals(token.value)) {
                parseSource();
            }
            else if (token.value.startsWith("source/")) {
                String classname = token.value.substring(7);
                parseSource(classname);
            }
            else if ("build".equals(token.value)) {
                parseBuild();
            }
            else if ("filter".equals(token.value)) {
                parseFilter();
            }
            else {
                throw new IOException("Unknown top level group: '"+token.value+"'");
            }
        }
    }

    private void parseRepository() throws IOException {
        while (readToken().type != TokenType.GROUP_CLOSE) {
            if (token.type != TokenType.LITERAL) {
                throw new IOException("Error reading repository. Expected: LITERAL; found: "+token);
            }
            model.addRepository(token.value);
        }
    }

    private void parseDepends() throws IOException {
        while (readToken().type != TokenType.GROUP_CLOSE) {
            if (token.type != TokenType.LITERAL) {
                throw new IOException("Error reading depends. Expected: LITERAL; found: "+token);
            }
            model.addDependency(token.value);
        }
    }

    private void parseSource() throws IOException {
        parseSource(null);
    }

    private void parseSource(String classname) throws IOException {
        String name = readLiteral();
        Source source = new Source(name, classname);
        String glob = readLiteral();
        source.setTemplate(glob);

        readToken();
        while (token.type != TokenType.GROUP_CLOSE) {
            if (token.type != TokenType.LITERAL) {
                throw new IOException("Error reading depends. Expected: LITERAL; found: "+token);
            }
            if (":param".equals(token.value) || ":parameter".equals(token.value)) {
                parseParameter(source);
            }
            else if (":depends".equals(token.value)) {
                parseDepends(source);
            }
            else {
                throw new IOException("Unknown keyword: "+token.value);
            }
        }
        model.addSource(source);
    }

    private void parseFilter() {
        throw new UnsupportedOperationException("Filter not supported");
    }

    private void parseBuild() throws IOException {
        String name = readLiteral();
        String classname = readLiteral();

        Build build = new Build(name, classname);

        readToken();
        while (token.type != TokenType.GROUP_CLOSE) {
            if (token.type != TokenType.LITERAL) {
                throw new IOException("Error reading depends. Expected: LITERAL; found: "+token);
            }
            if (":input".equals(token.value)) {
                parseInput(build);
            }
            else if (":output".equals(token.value)) {
                parseOutput(build);
            }
            else if (":param".equals(token.value) || ":parameter".equals(token.value)) {
                parseParameter(build);
            }
            else if (":depends".equals(token.value)) {
                parseDepends(build);
            }
            else {
                throw new IOException("Unknown keyword: "+token.value);
            }
        }
        model.addBuild(build);
    }

    private void parseInput(Build build) throws IOException {
        String step = readLiteral();
        readToken();
        if (token.type != TokenType.LITERAL || token.value.startsWith(":")) {
            build.addInput(new Input(step));
        }
        else {
            String name = step;
            step = token.value;
            build.addInput(new Input(name, step));
            readToken();
        }
    }
    
    private void parseOutput(Build build) throws IOException {
        String glob = readLiteral();
        readToken();
        if (token.type != TokenType.LITERAL || token.value.startsWith(":")) {
            build.addOutput(new Output(glob));
        }
        else {
            String name = glob;
            glob = token.value;
            build.addOutput(new Output(name, glob));
            readToken();
        }
    }

    private void parseParameter(Process build) throws IOException {
        String value = readLiteral();
        readToken();
        if (token.type != TokenType.LITERAL || token.value.startsWith(":")) {
            build.addParameter(new Parameter(value));
        }
        else {
            String key = value;
            value = token.value;
            build.addParameter(new Parameter(key, value));
            readToken();
        }
    }

    private void parseDepends(Process build) throws IOException {
        String d = readLiteral();
        int i0 = d.indexOf(':');
        int i1 = d.indexOf(':', i0+1);
        if (i0 == -1 || i1 == -1) {
            throw new IOException("Bad dependency. Expected groupId:artifactId:version; found "+d);
        }
        build.addDependency(d.substring(0,i0), d.substring(i0+1,i1), d.substring(i1+1));
        readToken();
    }


    private String readLiteral() throws IOException {
        if (readToken().type != TokenType.LITERAL) {
            throw new IOException("Error reading token; expected: LITERAL, found: "+token.type);
        }
        return token.value;
    }

    private LensfieldTokeniser.Token readToken() throws IOException {
        token = tokeniser.nextToken();
        return token;
    }

}
