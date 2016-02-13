package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.CacheDb;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public final class ListClassesCommand
    extends CachedbImportCommand
{
    public ListClassesCommand(final CacheDb db,
        final Map<String, String> arguments)
    {
        super(db, arguments);
    }

    @Override
    public void execute()
        throws CacheException, SQLException
    {
        final Set<String> set = runner.listClasses();
        set.forEach(System.out::println);
    }
}
