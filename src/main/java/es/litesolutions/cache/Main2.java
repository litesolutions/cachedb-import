package es.litesolutions.cache;

import com.intersys.cache.Dataholder;
import com.intersys.classes.GlobalCharacterStream;
import com.intersys.objects.CacheException;
import com.intersys.objects.Database;
import com.intersys.objects.StringHolder;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class Main2
{
    private static final String CACHEDB_HOST = "cachedb.host";
    private static final String CACHEDB_PORT = "cachedb.port";
    private static final String CACHEDB_USER = "cachedb.user";
    private static final String CACHEDB_PASSWORD = "cachedb.password";
    private static final String CACHEDB_NAMESPACE = "cachedb.namespace";
    private static final String LOADEDFILE = "loadedFile";

    private static final String CACHEDB_HOST_DEFAULT = "localhost";
    private static final String CACHEDB_PORT_DEFAULT = "1972";

    private static final String JDBC_URL_TEMPLATE = "jdbc:Cache://%s:%s/%s";

    private Main2()
    {
        throw new Error("instantiation not permitted");
    }

    public static void main(final String... args)
        throws IOException, CacheException
    {
        if (args.length == 0)
            throw new IllegalArgumentException("missing arguments");

        final Properties properties = new Properties();

        final Path path = Paths.get(args[0]).toRealPath();

        try (
            final Reader reader = Files.newBufferedReader(path);
        ) {
            properties.load(reader);
        }

        final String jdbcUrl = String.format(JDBC_URL_TEMPLATE,
            readProperty(properties, CACHEDB_HOST, CACHEDB_HOST_DEFAULT),
            readProperty(properties, CACHEDB_PORT, CACHEDB_PORT_DEFAULT),
            readProperty(properties, CACHEDB_NAMESPACE));

        final String user = readProperty(properties, CACHEDB_USER);
        final String password = readProperty(properties, CACHEDB_PASSWORD);

        final Path loadedFile = Paths.get(readProperty(properties, LOADEDFILE))
            .toRealPath();

        try (
            final CacheDb db = new CacheDb(jdbcUrl, user, password);
        ) {
            final GlobalCharacterStream stream
                = new GlobalCharacterStream(db.getDatabase());

            loadContent(stream, loadedFile);

            /*
             * Arguments for class "%SYSTEM.OBJ", class method "LoadStream"
             */
            final Dataholder[] arguments = new Dataholder[8];

            /*
             * Arguments ByRef
             *
             * Indices start at 1, not 0
             */
            final int[] byRefArgs = new int[2];

            // Arg 3: error log
            final StringHolder errorlog = new StringHolder("");
            byRefArgs[0] = 3;

            // Arg 4: list of loaded items
            final StringHolder loadedlist = new StringHolder("");
            byRefArgs[1] = 4;

            /*
             * Fill arguments
             */
            // arg 1: stream
            arguments[0] = Dataholder.create(stream);
            // arg 2: qspec; the default, therefore null
            arguments[1] = new Dataholder((String) null);
            // arg 3: errorlog
            arguments[2] = Dataholder.create(errorlog.value);
            // arg 4: loadedlist
            arguments[3] = Dataholder.create(loadedlist.value);
            // arg 5: listonly; we want true
            arguments[4] = Dataholder.create(Boolean.TRUE);
            // arg 6: selecteditems; nothing
            arguments[5] = Dataholder.create(null);
            // arg 7: displayname. For logging...
            arguments[6] = Dataholder.create("IMPORT");
            // arg 8: charset. Default is empty string, we'll assume UTF-8.
            arguments[7] = new Dataholder((String) null);

            // Now, make the call
            final Dataholder[] result = db.getDatabase().runClassMethod(
                "%SYSTEM.OBJ",
                "LoadStream",
                byRefArgs,
                arguments,
                Database.RET_PRIM
            );

            /*
             * The result normally has three members:
             *
             * - first is the status; and we need to do that:
             */
            db.getDatabase().parseStatus(result[0]);

            /*
             * - others are ByRef arguments
             */
            // FIXME: probably not ideal
            errorlog.set(result[1].getString());
            System.out.println("errorlog: " + errorlog.getValue());

            loadedlist.set(result[2].getString());
            System.out.println("loadedlist: " + loadedlist.getValue());
        }
    }

    private static void loadContent(final GlobalCharacterStream stream,
        final Path path)
        throws IOException, CacheException
    {
        final StringBuilder sb = new StringBuilder();

        try (
            final Reader reader = Files.newBufferedReader(path);
        ) {
            final char[] buf = new char[2048];
            int nrChars;

            while ((nrChars = reader.read(buf)) != -1)
                sb.append(buf, 0, nrChars);
        }

        stream._write(sb.toString());
    }

    private static String readProperty(final Properties properties,
        final String key)
    {
        final String ret = properties.getProperty(key);
        if (ret == null)
            throw new IllegalArgumentException("required property " + key
                + " is missing");
        return ret;
    }

    private static String readProperty(final Properties properties,
        final String key, final String defaultValue)
    {
        return properties.getProperty(key, defaultValue);
    }
}
