## What this is

The goal of this program is to take an XML export of a Caché project (as
exported by Studio, for instance) and generate a directory with plain source
files.

This program **needs not** run on the same machine as the Caché installation.

**Requires Java 8**. License is ASL 2.0.

## Status

Work in progress...

What is done right now is:

* list the available classes in a namespace;
* import an XML file.

Unfortunately, while the import works, [it appears to be impossible to obtain the
list of imported classes](http://stackoverflow.com/q/35360116/1093528).

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

