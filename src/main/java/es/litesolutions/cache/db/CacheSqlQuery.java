package es.litesolutions.cache.db;

import com.intersys.objects.CacheException;
import com.intersys.objects.CacheQuery;
import com.intersys.objects.CacheServerException;

import java.sql.ResultSet;

/**
 * An {@link AutoCloseable} wrapper over a {@link CacheQuery}
 */
public final class CacheSqlQuery
    implements AutoCloseable
{
    private final CacheQuery query;

    /**
     * Constructor
     *
     * @param query the query
     */
    public CacheSqlQuery(final CacheQuery query)
    {
        this.query = query;
    }

    /**
     * Get the JDBC {@link ResultSet} from this query
     *
     * @return the result set
     * @throws CacheException unable to obtain the result set
     */
    public ResultSet execute()
        throws CacheException
    {
        return query.execute();
    }

    @Override
    public void close()
        throws CacheServerException
    {
        query.close();
    }
}
