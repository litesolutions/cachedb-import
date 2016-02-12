package es.litesolutions.cache;

import com.github.fge.lambdas.functions.ThrowingFunction;
import com.intersys.cache.Dataholder;
import com.intersys.classes.CharacterStream;
import com.intersys.classes.Dictionary.ClassDefinition;
import com.intersys.classes.FileCharacterStream;
import com.intersys.classes.GlobalCharacterStream;
import com.intersys.objects.CacheException;
import com.intersys.objects.Database;
import com.intersys.objects.StringHolder;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A wrapper class for the necessary methods run by this program
 *
 * <p>This program takes a {@link CacheDb} as an argument (instead of a plain
 * {@link Database}) since the former's {@link CacheDb#query(ThrowingFunction)
 * query method} is needed.</p>
 */
public final class CacheRunner
{
    private static final String CLASSDEFINITION_NAME_SQLFIELD = "Name";

    private static final String LOADSTREAM_CLASSNAME = "%SYSTEM.OBJ";
    private static final String LOADSTREAM_METHODNAME = "LoadStream";

    private static final String LOADFILE_CLASSNAME = "%SYSTEM.OBJ";
    private static final String LOADFILE_METHODNAME = "Load";

    private final CacheDb cacheDb;

    public CacheRunner(final CacheDb cacheDb)
    {
        this.cacheDb = cacheDb;
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    public Set<String> listClasses()
        throws CacheException, SQLException
    {
        final Set<String> set = new HashSet<>();

        try (
            final CacheSqlQuery query
                = cacheDb.query(ClassDefinition::query_Summary);
            final ResultSet rs = query.execute();
        ) {
            while (rs.next())
                set.add(rs.getString(CLASSDEFINITION_NAME_SQLFIELD));
        }

        return Collections.unmodifiableSet(set);
    }

    public void importXml(final Path path)
        throws CacheException, IOException
    {
        final Database db = cacheDb.getDatabase();

        final CharacterStream stream = new GlobalCharacterStream(db);
        loadContent(stream, path);

        /*
         * Arguments for class "%SYSTEM.OBJ", class method "LoadStream"
         */
        final Dataholder[] arguments = new Dataholder[8];

        /*
         * Arguments ByRef
         *
         * Indices start at 1, not 0
         *
         * FIXME: in fact, they are unused...
         */
        final int[] byRefArgs = new int[2];

        // Arg 3: error log
        final StringHolder errorlog = new StringHolder("");
        byRefArgs[0] = 3;

        // Arg 4: list of loaded items
        final StringHolder loadedlist = new StringHolder("");
        byRefArgs[1] = 4;

            /*
             * Fill arguments
             */
        // arg 1: stream
        arguments[0] = Dataholder.create(stream);
        // arg 2: qspec; we want to ensure that compile works, at least
        arguments[1] = new Dataholder("c");
        // arg 3: errorlog
        arguments[2] = Dataholder.create(errorlog.value);
        // arg 4: loadedlist
        arguments[3] = Dataholder.create(loadedlist.value);
        // arg 5: listonly; no
        arguments[4] = Dataholder.create(Boolean.FALSE);
        // arg 6: selecteditems; nothing
        arguments[5] = Dataholder.create(null);
        // arg 7: displayname. For logging...
        arguments[6] = Dataholder.create("IMPORT");
        // arg 8: charset. Default is empty string, we'll assume UTF-8.
        arguments[7] = new Dataholder((String) null);

        // Now, make the call
        final Dataholder[] result = db.runClassMethod(
            LOADSTREAM_CLASSNAME,
            LOADSTREAM_METHODNAME,
            byRefArgs,
            arguments,
            Database.RET_PRIM
        );

        /*
         * The result normally has three members:
         *
         * - first is the status; and we need to do that:
         */
        db.parseStatus(result[0]);

        /*
         * - others are ByRef arguments
         */
        // FIXME: not filled correctly... No idea if this is a bug with
        // InterSystems jars or not. See the README for more details.
        // Ideally this should return a Set<String> containing the loaded
        // classes names...
//        errorlog.set(result[1].getString());
//        System.out.println("errorlog: " + errorlog.getValue());
//
//        loadedlist.set(result[2].getString());
//        System.out.println("loadedlist: " + loadedlist.getValue());
    }

    // DOESN'T WORK :(
    public void importXml2(final Path path)
        throws CacheException, IOException
    {
        final Database db = cacheDb.getDatabase();

        final FileCharacterStream stream = new FileCharacterStream(db);

        loadContent(stream, path);

        final String remoteFileName = stream._filenameGet();

        /*
         * Arguments for class "%SYSTEM.OBJ", class method "Load"
         */
        final Dataholder[] arguments = new Dataholder[9];

        /*
         * Arguments ByRef
         *
         * Indices start at 1, not 0
         */
        final int[] byRefArgs = new int[3];

        // Arg 3: error log
        final StringHolder errorlog = new StringHolder("");
        byRefArgs[0] = 3;

        // Arg 4: list of loaded items
        final StringHolder loadedlist = new StringHolder("");
        byRefArgs[1] = 4;

        // Arg 9: description (?)
        final StringHolder description = new StringHolder("");
        byRefArgs[2] = 9;

        /*
         * Fill arguments
         */
        // arg 1: file name
        arguments[0] = Dataholder.create(remoteFileName);
        // arg 2: qspec; we want to ensure that compile works, at least
        arguments[1] = new Dataholder("c");
        // arg 3: errorlog
        arguments[2] = Dataholder.create(errorlog.value);
        // arg 4: loadedlist
        arguments[3] = Dataholder.create(loadedlist.value);
        // arg 5: listonly; no
        arguments[4] = Dataholder.create(Boolean.FALSE);
        // arg 6: selecteditems; nothing
        arguments[5] = Dataholder.create(null);
        // arg 7: displayname. For logging...
        arguments[6] = Dataholder.create(null);
        // arg 8: charset. Default is empty string, we'll assume UTF-8.
        arguments[7] = new Dataholder((String) null);
        // arg 9: description (?)
        arguments[8] = Dataholder.create(description.value);

        // Now, make the call
        final Dataholder[] result = db.runClassMethod(
            LOADFILE_CLASSNAME,
            LOADFILE_METHODNAME,
            byRefArgs,
            arguments,
            Database.RET_PRIM
        );

        /*
         * The result normally has three members:
         *
         * - first is the status; and we need to do that:
         */
        db.parseStatus(result[0]);

        /*
         * - others are ByRef arguments
         */
        // FIXME: not filled correctly... No idea if this is a bug with
        // InterSystems jars or not. See the README for more details.
        // Ideally this should return a Set<String> containing the loaded
        // classes names...
//            errorlog.set(result[1].getString());
//            System.out.println("errorlog: " + errorlog.getValue());
//
//            loadedlist.set(result[2].getString());
//            System.out.println("loadedlist: " + loadedlist.getValue());

    }

    private static void loadContent(final CharacterStream stream,
        final Path path)
        throws IOException, CacheException
    {
        final StringBuilder sb = new StringBuilder();

        try (
            final Reader reader = Files.newBufferedReader(path);
        ) {
            final char[] buf = new char[2048];
            int nrChars;

            while ((nrChars = reader.read(buf)) != -1)
                sb.append(buf, 0, nrChars);
        }

        // Note that we don't _rewind() the stream; the loading function does
        // that by itself
        stream._write(sb.toString());
    }
}
