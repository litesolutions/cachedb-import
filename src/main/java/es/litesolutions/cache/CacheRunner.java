package es.litesolutions.cache;

import com.intersys.cache.Dataholder;
import com.intersys.classes.CharacterStream;
import com.intersys.classes.FileBinaryStream;
import com.intersys.classes.GlobalCharacterStream;
import com.intersys.objects.CacheException;
import com.intersys.objects.Database;
import com.intersys.objects.StringHolder;
import es.litesolutions.cache.db.CacheDb;
import es.litesolutions.cache.db.CacheQueryProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A wrapper class for the necessary methods run by this program
 *
 * <p>This program takes a {@link CacheDb} as an argument (instead of a plain
 * {@link Database}) since the former's {@link CacheDb#query(CacheQueryProvider)
 * query method} is needed.</p>
 */
public class CacheRunner extends Runner
{
    private static final String CRLF = "\r\n";

    private final CacheDb cacheDb;
    final Database db;

    public CacheRunner(final Connection connection)
            throws CacheException
    {
        super(connection);
        this.cacheDb = new CacheDb(connection);
        this.db = cacheDb.getDatabase();
    }

    @Override
    public void importStream(final Path path)
        throws CacheException, IOException
    {
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
        arguments[1] = new Dataholder("d");
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
//        errorlog.set(result[1].getString());
//        System.out.println("errorlog: " + errorlog.getValue());
//
//        loadedlist.set(result[2].getString());
//        System.out.println("loadedlist: " + loadedlist.getValue());
    }

    @Override
    public Set<String> importFile(final Path path, final boolean includeSys)
        throws CacheException, IOException
    {
        System.out.printf("Import from '%s'\n", path);
        final String tempFileName = createRemoteTemporaryFileName("xml");

        final Database db = cacheDb.getDatabase();

        final FileBinaryStream stream = new FileBinaryStream(db);
        stream._filenameSet(tempFileName);

        Files.copy(path, stream.getOutputStream());

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
        arguments[1] = new Dataholder("d");
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
        // arg 8: charset. We force UTF-8.
        arguments[7] = new Dataholder("UTF8");
        // arg 9: description (?)
        arguments[8] = Dataholder.create(description.value);

        try {
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
            // TODO
//        errorlog.set(result[1].getString());
//        System.out.println("errorlog: " + errorlog.getValue());

            loadedlist.set(result[2].getString());
        } catch (Exception ex) {
            System.out.println("Error loading file: " + ex.getLocalizedMessage());
            return Collections.emptySet();
        }

        Predicate<String> predicate = FILES;
        if (!includeSys)
            predicate = predicate.and(SYSEXCLUDE);

        final String value = (String) loadedlist.getValue();
        /*
         * It can happen if we have nothing imported...
         */
        if (value == null)
            return Collections.emptySet();
        final Set<String> set = COMMA.splitAsStream(value)
            .filter(predicate)
//            .map(s -> s.substring(0, s.length() - 4))
            .collect(Collectors.toCollection(HashSet::new));
        return Collections.unmodifiableSet(set);
    }

    @Override
    public void writeClassContent(final String itemName, final Path path)
        throws CacheException, IOException
    {
        final String tempFileName = createRemoteTemporaryFileName("txt");

        int versionMajor = 2016;
        int versionMinor = 2;
        try {
            versionMajor = connection.getMetaData().getDatabaseMajorVersion();
            versionMinor = connection.getMetaData().getDatabaseMinorVersion();
        } catch (Exception e) {
        }
        if (versionMajor < 2016 || (versionMajor == 2016 && versionMinor < 2)) {
            if (itemName.toLowerCase().endsWith("cls")) {
                System.out.printf("Export '%s' to '%s'\n", itemName, path);

                String className = itemName.substring(0, itemName.length() - 4);

                final int[] byRefs = new int[0];

                final Dataholder[] arguments = new Dataholder[3];
                arguments[0] = new Dataholder((String) null);
                arguments[1] = new Dataholder(className);
                arguments[2] = new Dataholder(tempFileName);

                final Dataholder[] res = db.runClassMethod(
                        "%Compiler.UDL.TextServices",
                        "GetTextAsFile",
                        byRefs,
                        arguments,
                        Database.RET_PRIM
                );

                db.parseStatus(res[0]);
            }
        } else {
            System.out.printf("Export '%s' to '%s'\n", itemName, path);


            final int[] byRefs = new int[0];

            final Dataholder[] arguments = new Dataholder[2];
            arguments[0] = new Dataholder(itemName);
            arguments[1] = new Dataholder(tempFileName);

            final Dataholder[] res = db.runClassMethod(
                    WRITECLASSCONTENT_CLASSNAME,
                    WRITECLASSCONTENT_METHODNAME,
                    byRefs,
                    arguments,
                    Database.RET_PRIM
            );

            db.parseStatus(res[0]);
        }

        final FileBinaryStream stream = new FileBinaryStream(db);
        stream._filenameSet(tempFileName);

        Files.copy(stream.getInputStream(), path);

        stream._clear();
    }

    private String createRemoteTemporaryFileName(String fileExt)
            throws CacheException
    {
        final Dataholder[] args = { new Dataholder(fileExt) };
        final Dataholder res = cacheDb.getDatabase()
                .runClassMethod(FILE_CLASSNAME, FILE_METHODNAME, args, 0);
        return res.getString();
    }

}
