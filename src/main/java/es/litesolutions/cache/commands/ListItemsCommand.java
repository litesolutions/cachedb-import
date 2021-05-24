package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * ListItems command: list COS classes from a database
 */
public final class ListItemsCommand
    extends CachedbCommand
{
    public ListItemsCommand(final Connection connection, String restUrl,
                            final Map<String, String> arguments)
            throws CacheException, IOException
    {
        super(connection, restUrl, arguments);
    }

    @Override
    public void execute()
        throws SQLException, IOException
    {
        final Set<String> set = runner.listItems(includeSys);
        set.stream().sorted().forEach(System.out::println);
    }
}
