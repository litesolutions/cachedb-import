package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.*;

import java.io.IOException;
import java.sql.Connection;
import java.util.Map;

/**
 * Abstract class for a command run by {@link CacheImport}
 */
public abstract class CachedbCommand
{
    protected static final String INCLUDESYS = "includeSys";
    protected static final String INCLUDESYS_DEFAULT = "true";

    protected final Runner runner;
    protected final Map<String, String> arguments;

    protected final boolean includeSys;

    /**
     * Constructor
     *
     * @param connection the database
     * @param arguments map of arguments
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    protected CachedbCommand(final Connection connection, String restUrl,
        final Map<String, String> arguments)
        throws IOException, CacheException
    {
        runner = connection == null ? new RESTRunner(restUrl, arguments) : new CacheRunner(connection);
        this.arguments = arguments;

        includeSys = Boolean.parseBoolean(Util.readOrDefault(INCLUDESYS,
            arguments, INCLUDESYS_DEFAULT));
    }

    /**
     * Execute this command
     *
     * @throws Exception refined in each individual command
     */
    public abstract void execute()
        throws Exception;

    protected final String getArgument(final String name)
    {
        return Util.readArgument(name, arguments);
    }

    protected final String getArgumentOrDefault(final String name,
        final String defaultValue)
    {
        return Util.readOrDefault(name, arguments, defaultValue);
    }
}
