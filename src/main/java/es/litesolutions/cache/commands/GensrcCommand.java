package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.db.CacheDb;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * Gensrc command: import into the database, then read back as source
 *
 * <p>In fact, this command makes use of both an {@link ImportCommand} and an
 * {@link ExportCommand}.</p>
 */
public final class GensrcCommand
    extends CachedbCommand
{
    private final ImportCommand importCommand;
    private final ExportCommand exportCommand;

    public GensrcCommand(final CacheDb cacheDb,
        final Map<String, String> arguments)
    {
        super(cacheDb, arguments);
        importCommand = new ImportCommand(cacheDb, arguments);
        exportCommand = new ExportCommand(cacheDb, arguments);
    }

    @Override
    public void execute()
        throws IOException, CacheException, SQLException
    {
        final Set<String> classes = importCommand.importAndList();
        exportCommand.prepareDirectory();
        exportCommand.writeClasses(classes);
    }
}
