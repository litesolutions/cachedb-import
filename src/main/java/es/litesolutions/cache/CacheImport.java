package es.litesolutions.cache;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.commands.CachedbCommand;
import es.litesolutions.cache.commands.ExportCommand;
import es.litesolutions.cache.commands.GensrcCommand;
import es.litesolutions.cache.commands.ImportCommand;
import es.litesolutions.cache.commands.ListItemsCommand;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The entry point
 */
public final class CacheImport
{
    private static final String HOST = "host";
    private static final String HOST_DEFAULT = "localhost";

    private static final String PORT = "port";
    private static final String PORT_DEFAULT = "1972";

    private static final String WEBPORT = "webport";
    private static final String WEBPORT_DEFAULT = "";

    private static final String WEBUSESSL = "webusessl";
    private static final String WEBUSESSL_DEFAULT = "false";

    private static final String USER = "user";

    private static final String PASSWORD = "password";

    private static final String NAMESPACE = "namespace";

    private static final String IRIS = "iris";

    private static final String JDBC_URL_TEMPLATE = "jdbc:%s://%s:%s/%s";

    private static final String REST_URL_TEMPLATE = "%s://%s:%s/api/atelier/v2/%s";

    private static final Map<String, CommandCreator> COMMANDS;

    static {
        final Map<String, CommandCreator> map = new HashMap<>();

        map.put("listItems", ListItemsCommand::new);
        map.put("export", ExportCommand::new);
        map.put("import", ImportCommand::new);
        map.put("gensrc", GensrcCommand::new);

        COMMANDS = Collections.unmodifiableMap(map);
    }

    private CacheImport()
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
        final String webport = Util.readOrDefault(WEBPORT, cfg, WEBPORT_DEFAULT);
        final boolean webusessl = Util.readOrDefault(WEBUSESSL, cfg, WEBUSESSL_DEFAULT).equals("true");
        final String namespace = Util.readOrDefault(NAMESPACE, cfg, "USER");

        final String product = Util.readOrDefault(IRIS, cfg, "").equals("true") ? "IRIS" : "Cache";

        if (webport.isEmpty()) {
            final String jdbcUrl = String.format(JDBC_URL_TEMPLATE, product, host, port, namespace);
            /*
             * Now the user and password
             */
            final String user = Util.readOrDefault(USER, cfg, "_SYSTEM");
            final String password = Util.readOrDefault(PASSWORD, cfg, "SYS");

            java.sql.Driver drv = java.sql.DriverManager.getDriver(jdbcUrl);

            java.util.Properties props = new Properties();
            props.put("user", user);
            props.put("password",password);
            try (
                    final Connection connection = drv.connect(jdbcUrl, props);
            ) {
                creator.create(connection, "", cfg).execute();
            }
        } else {
            final String restUrl = String.format(REST_URL_TEMPLATE, webusessl ? "https" : "http", host, webport, namespace);
            creator.create(null, restUrl, cfg).execute();
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
        CachedbCommand create(Connection connection, String restUrl, Map<String, String> cfg) throws CacheException, SQLException, IOException;
    }
}
