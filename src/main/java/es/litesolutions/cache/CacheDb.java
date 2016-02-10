package es.litesolutions.cache;

import com.intersys.objects.CacheDatabase;
import com.intersys.objects.CacheException;
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

    private static final String JDBC_URL_TEMPLATE = "jdbc:Cache://%s:%s/%s";

    private final Database database;

    private CacheDb(final String jdbcUrl, final String user,
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

    @Override
    public void close()
        throws CacheException
    {
        final int count = database.close().size();

        if (count > 0)
            LOGGER.error("{} objects not freed when closing database", count);
    }
}
