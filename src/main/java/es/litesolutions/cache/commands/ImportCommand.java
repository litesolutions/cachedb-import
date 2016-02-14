package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.db.CacheDb;
import es.litesolutions.cache.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class ImportCommand
    extends CachedbCommand
{
    private static final String MODE = "mode";
    private static final String FILE_MODE = "file";
    private static final String STREAM_MODE = "stream";

    private static final String INPUT_FILE = "inputFile";

    public ImportCommand(final CacheDb cacheDb,
        final Map<String, String> arguments)
    {
        super(cacheDb, arguments);
    }

    @Override
    public void execute()
        throws CacheException, IOException
    {
        final String file = getArgument(INPUT_FILE);
        final Path path = Paths.get(file);

        final String mode = getArgumentOrDefault(MODE, FILE_MODE);

        switch (mode) {
            case FILE_MODE:
                runner.importFile(path);
                break;
            case STREAM_MODE:
                runner.importStream(path);
                break;
            default:
                System.err.println("Unknown import mode " + mode);
                Util.readHelp("import");
                System.exit(2);
        }
    }
}
