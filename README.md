## What this is

This is a command line program which allows to perform the following:

* list classes from a given namespace;
* import an XML file into a namespace;
* export classes in a namespace as source files;
* combine both import and export: take an XML export, import it and generate the
  source files.

This program **needs not** run on the same machine as the Caché installation.

**Requires Java 8**. License is ASL 2.0.

## Status

The jar basically works; however, because InterSystems jars are not 
redistributable (that I know of, at least) and this program needs it, you have
to build it yourself; see the instructions below.

## Requisites

* Caché database 2016.2.

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
java -jar build/libs/cachedb-import.jar gensrc cfg=/tmp/db.properties
    inputFile=/tmp/myexport.xml outputDir=/tmp/sources
```

Please see the help for more options.

## Limitations

### Stream import (`mode=stream`) has limits

Unfortunately, the stream import routine provided by Caché is unable to provide
the list of classes which were imported. As a result, when you try and generate
the sources from an XML import, what happens is the following:

* the list of classes in the namespace is listed before import;
* the XML is imported;
* the list of classes after import is listed;
* the difference between those two lists is then exported.

### Imported items are not removed, even if only generating sources

One such reason is because of the stream import limitation mentioned above. If
you want to delete the imported items, you have to do it by hand.

### Only .(cls|mac|int|inc) are exported to sources

However, the import routine _will_ import everything in the XML (globals,
csp files, etc).

