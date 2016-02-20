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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;

/**
 * A set of utility methods, as the name says
 */
public final class Util
{
    private Util()
    {
        throw new Error("instantiation not permitted");
    }

    /**
     * Delete a directory recursively
     *
     * <p>Note that the first failure to delete a file/directory will stop the
     * deletion.</p>
     *
     * @param victim the directory to delete
     * @throws IOException failure to delete a file/directory
     */
    public static void deleteDirectory(final Path victim)
        throws IOException
    {
        final FileVisitor<Path> visitor = new RecursiveDeletion();
        Files.walkFileTree(victim, visitor);
    }

    /**
     * Read a help text from a given resource path
     *
     * @param cmdName the command
     * @throws IOException read failure
     */
    public static void readHelp(final String cmdName)
        throws IOException
    {
        final URL url = CacheImport.class.getResource('/' + cmdName + ".txt");

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

    /**
     * Read the main help text
     *
     * @throws IOException failure to read
     */
    public static void readHelp()
        throws IOException
    {
        readHelp("help");
    }

    /**
     * Read a required argument from a map
     *
     * <p>This method checks that both arguments are not null. The result is
     * guaranteed not to be null as well.</p>
     *
     * @param key the key in the map
     * @param map the map
     * @return the value
     * @throws IllegalArgumentException no such key in the map
     */
    public static String readArgument(final String key,
        final Map<String, String> map)
    {
        Objects.requireNonNull(key);
        Objects.requireNonNull(map);
        final String value = map.get(key);
        if (value == null)
            throw new IllegalArgumentException("required argument '" + key
                + "' is missing");
        return value;
    }

    /**
     * Read an optional argument from the map, returning a default if the key
     * is not found
     *
     * <p>All arguments must not be null. The result is also guaranteed not to
     * be null.</p>
     *
     * @param key the key
     * @param map the map
     * @param defaultValue the value to return if the key is not found
     * @return the value
     */
    public static String readOrDefault(final String key,
        final Map<String, String> map, final String defaultValue)
    {
        Objects.requireNonNull(key);
        Objects.requireNonNull(map);
        Objects.requireNonNull(defaultValue);
        return map.getOrDefault(key, defaultValue);
    }

    private static final class RecursiveDeletion
        extends SimpleFileVisitor<Path>
    {
        @Override
        public FileVisitResult visitFile(final Path file,
            final BasicFileAttributes attrs)
            throws IOException
        {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(final Path file,
            final IOException exc)
            throws IOException
        {
            throw exc;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir,
            final IOException exc)
            throws IOException
        {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }
}
