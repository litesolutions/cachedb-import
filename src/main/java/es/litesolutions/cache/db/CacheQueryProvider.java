package es.litesolutions.cache.db;

import com.intersys.objects.CacheException;
import com.intersys.objects.CacheQuery;
import com.intersys.objects.Database;

/**
 * Functional interface for {@link CacheDb#query(CacheQueryProvider)}
 */
@FunctionalInterface
public interface CacheQueryProvider
{
    CacheQuery getQuery(Database db)
        throws CacheException;
}
