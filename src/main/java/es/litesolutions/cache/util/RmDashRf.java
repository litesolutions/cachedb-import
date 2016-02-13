package es.litesolutions.cache.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class RmDashRf
{
    private RmDashRf()
    {
        throw new Error("no instantiation permitted");
    }

    public static void rmDashRf(final Path victim)
        throws IOException
    {
        final FileVisitor<Path> visitor = new RecursiveDeletion();
        Files.walkFileTree(victim, visitor);
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
