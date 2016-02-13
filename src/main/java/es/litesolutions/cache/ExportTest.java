package es.litesolutions.cache;

import com.github.fge.filesystem.MoreFiles;
import com.github.fge.filesystem.RecursionMode;
import com.intersys.objects.CacheException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

public final class ExportTest
{
    private static final Pattern DOT = Pattern.compile("\\.");

    private static final String CACHEDB_HOST = "cachedb.host";
    private static final String CACHEDB_PORT = "cachedb.port";
    private static final String CACHEDB_USER = "cachedb.user";
    private static final String CACHEDB_PASSWORD = "cachedb.password";
    private static final String CACHEDB_NAMESPACE = "cachedb.namespace";
    private static final String LOADEDFILE = "loadedFile";

    private static final String CACHEDB_HOST_DEFAULT = "localhost";
    private static final String CACHEDB_PORT_DEFAULT = "1972";

    private static final String JDBC_URL_TEMPLATE = "jdbc:Cache://%s:%s/%s";

    private ExportTest()
    {
        throw new Error("instantiation not permitted");
    }

    public static void main(final String... args)
        throws IOException, CacheException, SQLException
    {
        if (args.length < 2)
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

        final Path baseDir = Paths.get(args[1]).toAbsolutePath();

        if (Files.exists(baseDir))
            MoreFiles.deleteRecursive(baseDir, RecursionMode.FAIL_FAST);

        Files.createDirectories(baseDir);

        try (
            final CacheDb db = new CacheDb(jdbcUrl, user, password);
        ) {
            final CacheRunner runner = new CacheRunner(db);
            final Set<String> classes = runner.listClasses();

            Path out;

            for (final String className: classes) {
                out = computePath(baseDir, className);
                Files.createDirectories(out.getParent());
                runner.writeClassContent(className, out);
            }
        }
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

    private static Path computePath(final Path baseDir, final String className)
    {
        final String[] parts = DOT.split(className);
        final int len = parts.length;
        final String lastPart = parts[len - 1];

        Path ret = baseDir;

        for (int i = 0; i < len - 1; i++)
            ret = ret.resolve(parts[i]);

        return ret.resolve(lastPart + ".cls");
    }
}
