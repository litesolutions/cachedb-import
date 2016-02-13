package es.litesolutions.cache;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.commands.CachedbImportCommand;
import es.litesolutions.cache.commands.ListClassesCommand;
import es.litesolutions.cache.util.Argument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class CachedbImport
{
    private static final String HELP = "/help.txt";

    private static final String HOST = "host";
    private static final String HOST_DEFAULT = "localhost";

    private static final String PORT = "port";
    private static final String PORT_DEFAULT = "1972";

    private static final String USER = "user";

    private static final String PASSWORD = "password";

    private static final String NAMESPACE = "namespace";

    private static final String CFG = "cfg";
    private static final String CFG_DEFAULT = "cachedb.properties";

    private static final String JDBC_URL_TEMPLATE = "jdbc:Cache://%s:%s/%s";

    private static final Map<String, CommandCreator> COMMANDS;

    static {
        final Map<String, CommandCreator> map = new HashMap<>();

        map.put("listClasses", ListClassesCommand::new);

        COMMANDS = Collections.unmodifiableMap(map);
    }

    private CachedbImport()
    {
        throw new Error("instantiation not permitted");
    }

    public static void main(final String... args)
        throws IOException, CacheException, SQLException
    {
        if (args.length < 1) {
            readHelp();
            System.exit(2);
        }

        final String cmdName = args[0];
        final CommandCreator creator = COMMANDS.get(cmdName);

        if (creator == null) {
            System.err.printf("Unknown command '%s'%n", cmdName);
            readHelp();
            System.exit(2);
        }

        final Map<String, String> cfg = getCfg(args);

        final Path path = getProperties(cfg);

        if (path != null) {
            final Properties properties = new Properties();
            try (
                final Reader reader = Files.newBufferedReader(path);
            ) {
                properties.load(reader);
                for (final String key: properties.stringPropertyNames())
                    cfg.putIfAbsent(key, properties.getProperty(key));
            }
        } else
            System.err.printf("No properties file found; relying on command"
                + " line arguments");

        /*
         * Basic arguments required to generate the JDBC URL
         */
        final String host = Argument.readOrDefault(HOST, cfg, HOST_DEFAULT);
        final String port = Argument.readOrDefault(PORT, cfg, PORT_DEFAULT);
        final String namespace = Argument.read(NAMESPACE, cfg);

        final String jdbcUrl = String.format(JDBC_URL_TEMPLATE, host, port,
            namespace);

        /*
         * Now the user and password
         */
        final String user = Argument.read(USER, cfg);
        final String password = Argument.read(PASSWORD, cfg);

        try (
            final CacheDb db = new CacheDb(jdbcUrl, user, password);
        ) {
            creator.create(db, cfg).execute();
        }
    }

    private static Map<String, String> getCfg(final String[] args)
    {
        final Map<String, String> ret = new HashMap<>();

        String name;
        String value;
        int index;

        for (final String arg: args) {
            index = arg.indexOf('=');
            if (index == -1)
                continue;
            name = arg.substring(0, index);
            value = arg.substring(index + 1, arg.length());
            ret.put(name, value);
        }

        return ret;
    }

    private static Path getProperties(final Map<String, String> cfg)
    {
        final String candidate = cfg.getOrDefault(CFG, CFG_DEFAULT);

        final Path ret = Paths.get(candidate);

        return Files.isRegularFile(ret) ? ret : null;
    }

    private static void readHelp()
        throws IOException
    {
        final URL url = CachedbImport.class.getResource(HELP);

        if (url == null) {
            System.err.println("What the... Cannot find help text :(");
            System.exit(-1);
        }

        final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT);

        try (
            final InputStream in = url.openStream();
            final Reader reader = new InputStreamReader(in, decoder);
            final BufferedReader br = new BufferedReader(reader);
        ) {
            String line;

            while ((line = br.readLine()) != null)
                System.err.println(line);
        }
    }

    @FunctionalInterface
    private interface CommandCreator
    {
        CachedbImportCommand create(CacheDb cacheDb, Map<String, String> cfg);
    }
}
