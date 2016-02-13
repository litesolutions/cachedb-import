package es.litesolutions.cache.commands;

import es.litesolutions.cache.CacheDb;

import java.util.Map;
import java.util.Objects;

public abstract class CachedbImportCommand
{
    protected final CacheDb cacheDb;
    protected final Map<String, String> arguments;

    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    protected CachedbImportCommand(final CacheDb db,
        final Map<String, String> arguments)
    {
        cacheDb = db;
        this.arguments = arguments;
    }

    protected final String getArgument(final String name)
    {
        Objects.requireNonNull(name);
        final String value = arguments.get(name);
        if (value == null)
            throw new IllegalArgumentException("required argument " + name
                + " is missing");
        return value;
    }

    protected final String getArgumentOrDefault(final String name,
        final String defaultValue)
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(defaultValue);
        return arguments.getOrDefault(name, defaultValue);
    }
}
