package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;

import java.io.IOException;
import java.sql.Connection;
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

    public GensrcCommand(final Connection connection, String restUrl,
        final Map<String, String> arguments)
            throws CacheException, IOException
    {
        super(connection, restUrl, arguments);
        importCommand = new ImportCommand(connection, restUrl, arguments);
        exportCommand = new ExportCommand(connection, restUrl, arguments);
    }

    @Override
    public void execute()
        throws IOException, CacheException, SQLException
    {
        final Set<String> items = importCommand.importAndList();
        exportCommand.prepareDirectory();
        exportCommand.writeItems(items);
    }
}
