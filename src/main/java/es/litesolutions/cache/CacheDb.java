package es.litesolutions.cache;

import com.github.fge.lambdas.functions.ThrowingFunction;
import com.intersys.classes.Dictionary.ClassDefinition;
import com.intersys.objects.CacheDatabase;
import com.intersys.objects.CacheException;
import com.intersys.objects.CacheQuery;
import com.intersys.objects.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper over a Caché {@link Database}
 *
 * <p>The only reason for the existence of this class is that the class provided
 * by InterSystems does not implement {@link AutoCloseable}; this class does.
 * </p>
 *
 * @see Database#close()
 */
public final class CacheDb
    implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheDb.class);

    private final Database database;

    public CacheDb(final String jdbcUrl, final String user,
        final String password)
        throws CacheException
    {
        database = CacheDatabase.getDatabase(jdbcUrl, user, password);
    }

    /**
     * Get the underlying Caché {@link Database}
     *
     * @return the database
     */
    public Database getDatabase()
    {
        return database;
    }

    /**
     * Get a closeable query from the database
     *
     * @param function the function for obtaining the query (example: {@link
     * ClassDefinition#query_Summary(Database)}
     * @return the query
     */
    public CacheSqlQuery query(
        final ThrowingFunction<Database, CacheQuery> function)
    {
        final CacheQuery query = function.apply(database);
        return new CacheSqlQuery(query);
    }

    @Override
    public void close()
        throws CacheException
    {
        final int count = database.close().size();

        if (count > 0)
            LOGGER.error("{} objects not freed when closing database", count);
    }
}
