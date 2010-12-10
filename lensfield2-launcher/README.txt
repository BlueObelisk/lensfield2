
** Introduction **

Lensfield2 is a tool for managing file transformation workflows, analagous to
'make' for data.

Lensfield2 requires a build file, defining the various sets of input files
and the conversions to be applied to them. Like 'make', for instance,
Lensfield2 is able to detect when files have changed, and update the
products of conversions depending on them.  However, unlike 'make' where
this is simply achieved through comparison of files' last-modified times,
Lensfield2 records the complete build-state, so is able to detect if
there has been any change in configuration, such as when the parameterisation
of build steps has changed and when versions of tools involved in the various
steps of the workflow are updated, or when any intermediate files are altered.

Lensfield2 is designed to run workflow steps written in Java and build using
Apache Maven. Lensfield2 is able to tap into Maven's dependency management
system to pull in the required libraries for each build step.


** Installation **

Extract the contents of the Lensfield2 zip or tar.gz file into your filesystem,
and add the 'bin' directory to your PATH environment variable.


** Build steps **

Creating a Lensfield2 build step is very straightforward. In order to be used
as a step in a Lensfield2 build a class simply has to implement a 'run' method.
Various fields can be marked to take inputs, outputs or parameters using
annotations.

Build steps can be either 1:1, 1:n, or n:1 file conversions. Inputs can
either be Lensfield2 StreamIn or StreamOut objects, or MultiStreamIn or
MultiStreamOut objects. StreamIn and StreamOut extend java.io.InputStream and
java.io.OutputStream, respectively, and provide access to read from or write
to a single file, forming the '1' side of build steps. The MultiStream objects
give access to a number of Streams, and are used to create 'n/m' sided build
steps.


Examples:

* A 1:1 parameterised build step

public class Foo {

  @LensfieldInput
  private StreamIn input;

  @LensfieldOutput
  private StreamOut output;

  @LensfieldParameter
  private String param1;

  public void run() throws Exception { ... }

}


* A 1:n parameterised build step

public class Foo {

   @LensfieldInput
   private StreamIn input;

   @LensfieldOutput
   private MultiStreamOut output;

   @LensfieldParameter
   private String param1;

   public void run() throws Exception { ... }

}


* An n:1 parameterised build step

public class Foo {

   @LensfieldInput
   private MultiStreamIn input;

   @LensfieldOutput
   private StreamOut output;

   @LensfieldParameter
   private String param1;

   public void run() throws Exception { ... }

}

------

n:n conversions are not (currently) supported. However it is worth noting that
'1:n' here does not strictly mean a single input but rather a fixed number of
inputs/outputs, while 'n' means an indeterminate number of streams.

* e.g. A 3:2 build step

public class Foo {

   @LensfieldInput
   private StreamIn input1;

   @LensfieldInput
   private StreamIn input2;

   @LensfieldInput
   private StreamIn input3;

   @LensfieldOutput
   private StreamOut output1;

   @LensfieldOutput
   private StreamOut output2;

   public void run() throws Exception { ... }

}

Lensfield2 will initialise each of these streams automatically.

In order to implement any of these build steps only the small lensfield2-api
library needs to be included in your build path, however for the case of
unparameterised 1:1 builds there is an even simpler approach to forming
a Lensfield2 build. All that is required is the implementation of a method
with the signature:

  public X run(InputStream in, OutputStream out) throws Y;

This method may return void, or anything else, and can throw any classes of
exception.  The method does not even need to be named 'run', making it 
straightforward to construct a build step from any method taking a single
InputStream and a single OutputStream as arguments, even if the codebase
is unaware of Lensfield2's existence. (See the examples of using Lensfield2
with the Apache commons-io IOUtils class, below).


** Build file definitions **

Lensfield2 builds consist of a series of 'source' and 'build' steps. Source
steps introduce filesets into the build system, while build steps process
existing filesets, generating new filesets as a result.

Source and build steps require a unique identifier, which is used to name the
filesets they generate.

Source declarations take the form:
(source <id> <glob>)

Where:
  <id>    is the name of the resulting fileset
  <glob>  is the pattern specifying which files to include

e.g.
(source files **/*.xyz)

This finds all files with the extension 'xyz', and assigns them to a file
set named 'files'.


Build declarations take the form:
(build <id> <class-name> <arg>*)

Where:
  <id>         is the name of the resulting fileset

  <class-name> is the name of the class that carries out the process. If you
               wish Lensfield2 to invoke a method named other than 'run', then
               you can append it to the class-name, separated by a '/'.
                 e.g. com.example.legacytools.Copier/copy

  <arg>        is one of...

      :input   name? fileset-name
      :output  name? glob
      :param   name  value
      :depends groupId:artifactId:version

If a build step has more than one input, or more than one output, then the input/
output's names must be specified. If there is only a single input or output
then the name may be omitted.

Parameter names must always be specified.


In addition to dependencies for individual build steps, global dependencies applying
to all build steps can be specified:

(depends <groupId:artifactId:version>*)

In the case of any conflicts, dependencies assigned to a build step take priority
over global dependencies (i.e. they are placed earlier in the classpath).

Lensfield will automatically search for dependencies it the Maven central
repository (http://repo1.maven.org/maven2), and you can also specify further
locations:

(repository https://maven.ch.cam.ac.uk/m2repo)


Examples

--------------------------------------------------------------------------------
; Specify a repository to load dependencies from
(repository
    https://maven.ch.cam.ac.uk/m2repo)

; Declare a global dependency (applies to all build steps)
(depends
    org.lensfield.testing:lensfield2-testops1:0.2-SNAPSHOT)

; Define source files (each file *.n contains a number)
(source
    files       **/*.n)

; Calculate the double of the number in each file (1:1 conversion)
(build
    doubles     org.lensfield.testing.ops.number.Doubler
    :input      files
    :output     **/*.x2)

; Calculate the square of the number in each file (1:1 conversion)
(build
    doubles     org.lensfield.testing.ops.number.Squarer
    :input      files
    :output     **/*.sq)

; Calculate the sum of the doubles (n:1 conversion)
(build
    sum-x2      org.lensfield.testing.ops.number.Summer
    :input      doubles
    :output     sum-x2.txt)

; Calculate the sum of the squares (n:1 conversion)
(build
    sum-sq      org.lensfield.testing.ops.number.Summer
    :input      squares
    :output     sum-sq.txt)
--------------------------------------------------------------------------------
; Build file taking all files **/*.n, and copying each to **/*.nn
; Illustrates use of Lensfield2 'unaware' routines from the Apache
; commons-io library
(repository
    https://maven.ch.cam.ac.uk/m2repo)

(source
    files       **/*.n)

(build
    copies      org.apache.commons.io.IOUtils/copy
    :input      files
    :output     **/*.nn
    :depends    commons-io:commons-io:1.4)
--------------------------------------------------------------------------------
; Splits a text file line-by-line
; Illustates 1:n conversion
; Note: {%i} in output glob is a special parameter for MultiStreamOutputs
;       containing the number (1,2,3...) of the stream created.  
(repository
    https://maven.ch.cam.ac.uk/m2repo)

(depends
    org.lensfield.testing:lensfield2-testops1:0.2-SNAPSHOT)

(source
    files       input/*.txt)

(build
    split-files org.lensfield.testing.ops.file.Splitter
    :input      files
    :output     output/*-{%i}.txt)
--------------------------------------------------------------------------------
