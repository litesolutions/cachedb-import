package es.litesolutions.cache;

import es.litesolutions.cache.commands.CachedbCommand;
import es.litesolutions.cache.commands.ExportCommand;
import es.litesolutions.cache.commands.GensrcCommand;
import es.litesolutions.cache.commands.ImportCommand;
import es.litesolutions.cache.commands.ListClassesCommand;
import es.litesolutions.cache.db.CacheDb;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class CachedbImport
{
    private static final String HOST = "host";
    private static final String HOST_DEFAULT = "localhost";

    private static final String PORT = "port";
    private static final String PORT_DEFAULT = "1972";

    private static final String USER = "user";

    private static final String PASSWORD = "password";

    private static final String NAMESPACE = "namespace";

    private static final String JDBC_URL_TEMPLATE = "jdbc:Cache://%s:%s/%s";

    private static final Map<String, CommandCreator> COMMANDS;

    static {
        final Map<String, CommandCreator> map = new HashMap<>();

        map.put("listClasses", ListClassesCommand::new);
        map.put("export", ExportCommand::new);
        map.put("import", ImportCommand::new);
        map.put("gensrc", GensrcCommand::new);

        COMMANDS = Collections.unmodifiableMap(map);
    }

    private CachedbImport()
    {
        throw new Error("instantiation not permitted");
    }

    public static void main(final String... args)
        throws Exception
    {
        if (args.length < 1) {
            Util.readHelp();
            System.exit(2);
        }

        final String cmdName = args[0];
        final CommandCreator creator = COMMANDS.get(cmdName);

        if ("help".equals(cmdName)) {
            Util.readHelp();
            System.exit(2);
        }

        if (creator == null) {
            System.err.printf("Unknown command '%s'%n", cmdName);
            Util.readHelp();
            System.exit(2);
        }

        if (args.length >= 2 && "help".equals(args[1])) {
            Util.readHelp(cmdName);
            System.exit(2);
        }

        final Map<String, String> cfg = getCfg(args);

        final String cfgFile = cfg.get("cfg");

        if (cfgFile != null) {
            final Properties properties = new Properties();
            try (
                final Reader reader = Files.newBufferedReader(
                    Paths.get(cfgFile));
            ) {
                properties.load(reader);
                for (final String key: properties.stringPropertyNames())
                    cfg.putIfAbsent(key, properties.getProperty(key));
            }
        }

        /*
         * Basic arguments required to generate the JDBC URL
         */
        final String host = Util.readOrDefault(HOST, cfg, HOST_DEFAULT);
        final String port = Util.readOrDefault(PORT, cfg, PORT_DEFAULT);
        final String namespace = Util.readArgument(NAMESPACE, cfg);

        final String jdbcUrl = String.format(JDBC_URL_TEMPLATE, host, port,
            namespace);

        /*
         * Now the user and password
         */
        final String user = Util.readArgument(USER, cfg);
        final String password = Util.readArgument(PASSWORD, cfg);

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

    @FunctionalInterface
    private interface CommandCreator
    {
        CachedbCommand create(CacheDb cacheDb, Map<String, String> cfg);
    }
}
