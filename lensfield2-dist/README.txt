
** Installation **

Extract the contents of the lensfield zip/tar.gz file into your filesystem,
and add the 'bin' directory to your PATH environment variable.


** Build file definitions **

Lensfield builds consist if a series of 'source' and 'build' steps. Source
steps introduce filesets into the build system, while build steps process
existing filesets, generating new filesets as a result.

Source and build steps require a unique identifier, or handle, that is used
to name the filesets they generate.

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
  <class-name> is the name of the class that carries out the process
  <arg>        is one of...

      :input   name? fileset-name
      :output  name? fileset-name
      :param   name  value
      :depends groupId:artifactId:version


