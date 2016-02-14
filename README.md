## What this is

The goal of this program is to take an XML export of a Caché project (as
exported by Studio, for instance) and generate a directory with plain source
files.

This program **needs not** run on the same machine as the Caché installation.

**Requires Java 8**. License is ASL 2.0.

## Status

The jar basically works; however, because InterSystems jars are not 
redistributable (that I know of, at least) and this program needs it, you have
to build it yourself; see the instructions below.

## How to use

* Clone this project.
* Have a JDK 8 install.
* Copy `cachedb.jar` and `cachejdbc.jar` from a Caché installation into the
  `lib` directory of the cloned project.
* Build the jar with `./gradlew shadowJar`.

## Running

The generated jar is fully contained, and generated as 
`build/libs/cachedb-import.jar`.

You can view a global help with:

```
java -jar build/libs/cachedb-import.jar help
```

It is recommended that you use a property file with the following keys, so that
the command line is not too long, and reuse it across your commands:


```
# Host where the Caché installation is.
# If not specified, the default is localhost.
host = some.host.somewhere
# Port to connect to.
# If not specified, the default is 1972.
port = 1972
# REQUIRED: Caché user
user = theuser
# REQUIRED: the password
password = thepassword
# REQUIRED: the namespace
namespace = THENAMESPACE
```

Say this file is named `/tmp/db.properties`; if you have, for instance, an XML
export file named `/tmp/myexport.xml` and want to generate the sources in
directory `/tmp/sources`, you will use this command line:

```
java -jar build/libs/cachedb-import.jar cfg=/tmp/db.properties
    inputFile=/tmp/myexport.xml outputDir=/tmp/sources
```

Please see the help for more options.

## How this works

### Listing the classes

This is done using the summary query of `%Library.ClassDefinition`.

**Note that this means that only classes (ie, .cls) files are accounted for!**
Include files and MAC files are not, nor are project files.

### Import as a stream

In this case, the streaming load method of `$SYSTEM.OBJ` is used. Unfortunately,
in this case, the list of loaded objects is... Nothing.

Which means that in order to list the classes actually loaded by the imported
XML, an alternate solution needs to be found. The current plan is as follows:

* list the classes before import;
* import as a stream;
* list the classes after import;
* compute the difference: those are the loaded classes.

### Import using a remote file

In this case, a remote file is created on the server; the contents of the
imported file are copied into it, and the file load method of `$SYSTEM.OBJ` is
called.

And with this method, you do get the list of classes which were imported, unlike
with the streaming load method.

### Exporting to source files

The `%Compiler.UDL.TextServices` class is used in this case.

This class allows the source to be written in exactly the same manner as you
would see them in Studio.

## Problems

### Unable to use the file import with some environments

This environment is known to completely fail with file based import, for reasons
still not understood:

* Windows 8.1, x86_64;
* Caché 2015.3.

The problems with this environment are, along others:

* if a character stream is used, the result file on the server is corrupted;
* if a binary stream is used, the result file is not corrupted, but the import
  fails.

Which means that for this environment in particular, and maybe others, there is
no choice but to use the streaming import -- at least until the reasons for
these failures are understood and possibly fixed.

