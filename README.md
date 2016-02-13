## What this is

The goal of this program is to take an XML export of a Caché project (as
exported by Studio, for instance) and generate a directory with plain source
files.

This program **needs not** run on the same machine as the Caché installation.

**Requires Java 8**. License is ASL 2.0.

## Status

Work in progress...

What is done right now is:

* list the available classes in a namespace (this is done using [the Java
  binding for this
class](http://docs.intersystems.com/cache20152/csp/documatic/%25CSP.Documatic.cls?PAGE=CLASS&LIBRARY=%25SYS&CLASSNAME=%25Dictionary.ClassDefinitionQuery));
* import an XML file as a stream (this is done using [this
  method](http://docs.intersystems.com/cache20152/csp/documatic/%25CSP.Documatic.cls?PAGE=CLASS&LIBRARY=%25SYS&CLASSNAME=%25SYSTEM.OBJ#METHOD_LoadStream))
  or using a remote created file (using [this method](http://docs.intersystems.com/cache20152/csp/documatic/%25CSP.Documatic.cls?PAGE=CLASS&LIBRARY=%25SYS&CLASSNAME=%25SYSTEM.OBJ#METHOD_Load));
* write a class to a plain file (this is done using [this
  method](http://docs.intersystems.com/cache20152/csp/documatic/%25CSP.Documatic.cls?PAGE=CLASS&LIBRARY=%25SYS&CLASSNAME=%25Compiler.UDL.TextServices#METHOD_GetTextAsString)).

Read the "Problems" section below... There are unfortunately quite a few if the
Caché installation is under Windows.

## How to hack on it

* Clone this project.
* Have a JDK 8 install.
* Copy `cachedb.jar` and `cachejdbc.jar` from a Caché installation into the
  `lib` directory of the cloned project.
* Open this project with your favorite IDE (which must support
  [gradle](http://www.gradle.org)).
* On the Caché side:
    * create an empty namespace;
    * create a user (or reuse an existing one) for that namespace; note that
      this user must have execution privileges on the `%SYS` namespace.

Once the above is done, create a properties files with the following keys:


```
# Host where the Caché installation is.
# If not specified, the default is localhost.
cachedb.host = some.host.somewhere
# Port to connect to.
# If not specified, the default is 1972.
cachedb.port = 1972
# REQUIRED: Caché user
cachedb.user = theuser
# REQUIRED: the password
cachedb.password = thepassword
# REQUIRED: the namespace
cachedb.namespace = THENAMESPACE
# REQUIRED if importing: the file to load
loadedFile = /path/to/some/file
```

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

