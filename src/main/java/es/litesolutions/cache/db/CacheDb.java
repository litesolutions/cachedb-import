package es.litesolutions.cache.db;

import com.intersys.cache.Dataholder;
import com.intersys.classes.Dictionary.ClassDefinition;
import com.intersys.objects.CacheDatabase;
import com.intersys.objects.CacheException;
import com.intersys.objects.CacheQuery;
import com.intersys.objects.Database;

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
    private static final String CLASSNAME = "%Library.MessageDictionary";
    private static final String METHODNAME = "SetSessionLanguage";
    private static final String LANG = "en-us";

    private final Database database;

    public CacheDb(final String jdbcUrl, final String user,
        final String password)
        throws CacheException
    {
        database = CacheDatabase.getDatabase(jdbcUrl, user, password);

        /*
         * Set the session language to English
         */
        final Dataholder[] holders = { Dataholder.create(LANG) };
        database.runClassMethod(CLASSNAME, METHODNAME, holders,
            Database.RET_NONE);
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
     * @param provider the function for obtaining the query (example: {@link
     * ClassDefinition#query_Summary(Database)}
     * @return the query
     */
    public CacheSqlQuery query(final CacheQueryProvider provider)
        throws CacheException
    {
        final CacheQuery query = provider.getQuery(database);
        return new CacheSqlQuery(query);
    }

    @Override
    public void close()
        throws CacheException
    {
        final int count = database.close().size();

        if (count > 0)
            System.err.printf("%d objects not freed when closing database",
                count);
    }
}
