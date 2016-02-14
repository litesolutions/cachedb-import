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

public final class Util
{
    private Util()
    {
        throw new Error("instantiation not permitted");
    }

    public static void deleteDirectory(final Path victim)
        throws IOException
    {
        final FileVisitor<Path> visitor = new RecursiveDeletion();
        Files.walkFileTree(victim, visitor);
    }

    public static void readHelp(final String cmdName)
        throws IOException
    {
        final URL url = CachedbImport.class.getResource('/' + cmdName + ".txt");

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

    public static void readHelp()
        throws IOException
    {
        readHelp("help");
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
