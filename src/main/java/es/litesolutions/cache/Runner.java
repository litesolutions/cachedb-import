package es.litesolutions.cache;

import com.intersys.classes.CharacterStream;
import com.intersys.objects.CacheException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public abstract class Runner {

    Predicate<String> FILES = s -> s.toLowerCase().matches(".*\\.(cls|mac|int|inc)$");
    Predicate<String> SYSEXCLUDE = s -> s.charAt(0) != '%';

    Pattern COMMA = Pattern.compile(",");

    String LOADSTREAM_CLASSNAME = "%SYSTEM.OBJ";
    String LOADSTREAM_METHODNAME = "LoadStream";

    String LOADFILE_CLASSNAME = "%SYSTEM.OBJ";
    String LOADFILE_METHODNAME = "Load";

    String WRITECLASSCONTENT_CLASSNAME = "%SYSTEM.OBJ";
    String WRITECLASSCONTENT_METHODNAME = "ExportUDL";

    String FILE_CLASSNAME = "%File";
    String FILE_METHODNAME = "TempFilename";

    final Connection connection;

    public Runner(Connection connection)
    {
        this.connection = connection;
    }

    /**
     * List the classes for this database
     *
     * @param includeSys also list system classes
     * @return the set of classes
     * @throws SQLException SQL error
     */
    public Set<String> listItems(final boolean includeSys)
            throws SQLException, IOException
    {
        final Set<String> set = new HashSet<>();

        final String sql = "SELECT Name FROM %Library.RoutineMgr_StudioOpenDialog(?,,,?,1,0,0)";
        try (
                final PreparedStatement ps = connection.prepareStatement(sql);
        ) {
            ps.setString(1, "*.cls,*.inc,*.mac,*.int");
            ps.setBoolean(2, includeSys);
            final ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                final String name = rs.getString("Name");
                set.add(name);
            }
        }

        return Collections.unmodifiableSet(set);
    }

    /**
     * Import an XML export as a stream
     *
     * <p>This is needed in some situations where the file import does not work.
     * Unfortunately, with this mode, you cannot reliably get a list of loaded
     * classes. See <a
     * href="http://stackoverflow.com/a/35371306/1093528">here</a> for more
     * details.</p>
     *
     * <p>Prefer to use {@link #importFile(Path, boolean)} instead.</p>
     *
     * @param path path to the XML export
     * @throws CacheException Caché error
     * @throws IOException Failed to read from the XML export
     */
    public void importStream(final Path path)
            throws CacheException, IOException
    {
        throw new Error("Not implemented");
    }

    /**
     * Import an XML as a file
     *
     * <p>In fact, this creates a file using the Caché API on the remote server,
     * copies the content of the XML to this file and then imports it. Unlike
     * what happens with {@link #importStream(Path)}, with this method, you
     * <em>do</em> get the list of loaded items back.</p>
     *
     * @param path path to the XML to import
     * @param includeSys include system classes
     * @return the list of loaded classes
     * @throws CacheException Caché error
     * @throws IOException Failure to read from the XML export
     */
    public Set<String> importFile(final Path path, final boolean includeSys)
            throws CacheException, IOException
    {
        throw new Error("Not implemented");
    }

    /**
     * Write the source code of a Caché class to a file
     *
     * <p>The file is written using UTF-8 and {@code \r\n} as a line terminator.
     * </p>
     *
     * @param className the class name
     * @param path path of the file to write
     * @throws CacheException Caché error
     * @throws IOException Write failure
     */
    public void writeClassContent(final String className, final Path path)
            throws CacheException, IOException
    {
        throw new Error("Not implemented");
    }

    protected static void loadContent(final CharacterStream stream,
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

