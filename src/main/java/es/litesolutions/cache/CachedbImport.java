package es.litesolutions.cache;

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

    public static void main(final String... args)
        throws IOException
    {
        if (args.length < 1) {
            readHelp();
            System.exit(2);
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

        return Collections.unmodifiableMap(ret);
    }

    private static Map<String, String> readProperties(
        final Properties properties)
    {
        final Map<String, String> ret = new HashMap<>();

        for (final String name: properties.stringPropertyNames())
            ret.put(name, properties.getProperty(name));

        return Collections.unmodifiableMap(ret);
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
}
