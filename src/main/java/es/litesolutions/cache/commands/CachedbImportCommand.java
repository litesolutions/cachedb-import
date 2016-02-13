package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.CacheDb;
import es.litesolutions.cache.CacheRunner;
import es.litesolutions.cache.util.Argument;

import java.sql.SQLException;
import java.util.Map;

public abstract class CachedbImportCommand
{
    protected final CacheDb cacheDb;
    protected final CacheRunner runner;
    protected final Map<String, String> arguments;

    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    protected CachedbImportCommand(final CacheDb cacheDb,
        final Map<String, String> arguments)
    {
        this.cacheDb = cacheDb;
        runner = new CacheRunner(cacheDb);
        this.arguments = arguments;
    }

    public abstract void execute()
        throws CacheException, SQLException;

    protected final String getArgument(final String name)
    {
        return Argument.read(name, arguments);
    }

    protected final String getArgumentOrDefault(final String name,
        final String defaultValue)
    {
        return Argument.readOrDefault(name, arguments, defaultValue);
    }
}
