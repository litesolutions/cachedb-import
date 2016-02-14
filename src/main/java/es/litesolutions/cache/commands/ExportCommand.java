package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.db.CacheDb;
import es.litesolutions.cache.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ExportCommand
    extends CachedbCommand
{
    private static final Pattern DOT = Pattern.compile("\\.");

    private static final String OUTPUTDIR = "outputDir";

    private static final String OVERWRITE = "overwrite";
    private static final String OVERWRITE_DEFAULT = "false";

    public ExportCommand(final CacheDb cacheDb,
        final Map<String, String> arguments)
    {
        super(cacheDb, arguments);
    }

    @Override
    public void execute()
        throws CacheException, SQLException, IOException
    {
        final String dir = getArgument(OUTPUTDIR);
        final Path outputDir = Paths.get(dir).toAbsolutePath();

        final String s = getArgumentOrDefault(OVERWRITE, OVERWRITE_DEFAULT);
        final boolean overwrite = Boolean.parseBoolean(s);

        if (Files.exists(outputDir)) {
            if (!overwrite) {
                System.err.printf("directory %s already exists", outputDir);
                System.exit(2);
            }
            Util.deleteDirectory(outputDir);
        }

        Files.createDirectories(outputDir);

        final Set<String> classes = runner.listClasses();

        Path out;

        for (final String className: classes) {
            out = computePath(outputDir, className);
            Files.createDirectories(out.getParent());
            runner.writeClassContent(className, out);
        }
    }

    private static Path computePath(final Path outputDir,
        final String className)
    {
        final String[] parts = DOT.split(className);
        final int len = parts.length;
        final String lastPart = parts[len - 1];

        Path ret = outputDir;

        for (int i = 0; i < len - 1; i++)
            ret = ret.resolve(parts[i]);

        return ret.resolve(lastPart + ".cls");
    }
}
