package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.db.CacheDb;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ImportCommand
    extends CachedbCommand
{
    private static final String MODE = "mode";
    private static final String FILE_MODE = "file";
    private static final String STREAM_MODE = "stream";

    private static final String INPUT_FILE = "inputFile";

    private final Path inputFile;
    private final String mode;

    public ImportCommand(final CacheDb cacheDb,
        final Map<String, String> arguments)
    {
        super(cacheDb, arguments);
        inputFile = Paths.get(getArgument(INPUT_FILE)).toAbsolutePath();
        mode = getArgumentOrDefault(MODE, FILE_MODE);

        switch (mode) {
            case FILE_MODE:
            case STREAM_MODE:
                return;
            default:
                System.err.println("Unknown import mode " + mode);
                System.exit(2);
        }
    }

    @Override
    public void execute()
        throws CacheException, IOException
    {
        switch (mode) {
            case FILE_MODE:
                importFile();
                break;
            case STREAM_MODE:
                importStream();
                break;
            default:
                throw new Error("Unreachable; how did I get there?");
        }
    }

    Set<String> importAndList()
        throws CacheException, IOException, SQLException
    {
        switch (mode) {
            case FILE_MODE:
                return importFile();
            case STREAM_MODE:
                return importStreamAndList();
            default:
                throw new Error("Unreachable; how did I get there?");
        }
    }

    private Set<String> importFile()
        throws CacheException, IOException
    {
        return runner.importFile(inputFile);
    }

    private Set<String> importStreamAndList()
        throws CacheException, SQLException, IOException
    {
        final Set<String> before = runner.listClasses();
        importStream();
        final Set<String> after = new HashSet<>(runner.listClasses());
        after.removeAll(before);
        return Collections.unmodifiableSet(after);
    }

    private void importStream()
        throws CacheException, IOException
    {
        runner.importStream(inputFile);
    }
}
