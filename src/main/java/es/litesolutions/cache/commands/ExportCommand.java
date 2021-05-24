package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Export command: write the source of all classes/routines to a given directory
 */
public final class ExportCommand
    extends CachedbCommand
{
    private static final Pattern DOT = Pattern.compile("\\.");

    private static final String OUTPUTDIR = "outputDir";

    private static final String OVERWRITE = "overwrite";
    private static final String OVERWRITE_DEFAULT = "false";

    private final Path outputDir;
    private final boolean overwrite;

    public ExportCommand(final Connection connection, String restUrl,
        final Map<String, String> arguments)
            throws CacheException, IOException
    {
        super(connection, restUrl, arguments);
        outputDir = Paths.get(getArgument(OUTPUTDIR)).toAbsolutePath();
        overwrite = Boolean.parseBoolean(getArgumentOrDefault(OVERWRITE,
            OVERWRITE_DEFAULT));
    }

    @Override
    public void execute()
        throws CacheException, SQLException, IOException
    {
        prepareDirectory();

        final Set<String> items = runner.listItems(includeSys);

        writeItems(items);
    }

    void prepareDirectory()
        throws IOException
    {
        if (Files.exists(outputDir)) {
            if (!overwrite) {
                System.err.printf("directory %s already exists", outputDir);
                System.exit(2);
            }
            Util.deleteDirectory(outputDir);
        }

        Files.createDirectories(outputDir);
    }

    void writeItems(final Set<String> items)
        throws CacheException, IOException
    {
        Path out;

        for (final String itemName: items) {
            final String itemFileName = itemName;
            out = computePath(itemFileName);
            Files.createDirectories(out.getParent());
            runner.writeClassContent(itemFileName, out);
        }
    }

    private Path computePath(final String itemName) throws IOException
    {
        final String[] parts = DOT.split(itemName);
        final String ext = parts[parts.length - 1];
        final int len = parts.length - 1;
        final String lastPart = parts[len - 1];

        Path ret = outputDir;

        for (int i = 0; i < len - 1; i++)
            ret = ret.resolve(parts[i]);

        return ret.resolve(lastPart + '.' + ext);
    }
}
