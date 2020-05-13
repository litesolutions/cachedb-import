package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Import command: import an XML into the database
 */
public final class ImportCommand
    extends CachedbCommand
{

    private static final String MODE = "mode";
    private static final String FILE_MODE = "file";
    private static final String STREAM_MODE = "stream";

    private static final String INPUT_FILE = "inputFile";
    private static final String INPUT_DIR = "inputDir";

    private final List<Path> files;
    private final String mode;

    public ImportCommand(final Connection connection, String restUrl,
        final Map<String, String> arguments)
            throws CacheException
    {
        super(connection, restUrl, arguments);

        final String inputFileString = getArgumentOrDefault(INPUT_FILE, "");
        final Path inputFile = inputFileString == "" ? null : Paths.get(inputFileString).toAbsolutePath();
        final String inputDirString = getArgumentOrDefault(INPUT_DIR, "");
        final Path inputDir = inputDirString == "" ? null : Paths.get(inputDirString).toAbsolutePath();

        files = collectPaths(inputFile, inputDir);

        mode = getArgumentOrDefault(MODE, FILE_MODE);

        switch (mode) {
            case FILE_MODE:
            case STREAM_MODE:
                return;
            default:
                System.err.println("Unknown import mode " + mode);
                System.exit(2);
        }
    }

    public ImportCommand(String restUrl, Map<String, String> arguments) throws CacheException, SQLException
    {
        this(null, restUrl, arguments);
    }

    public ImportCommand(Connection connection, Map<String, String> arguments) throws CacheException, SQLException
    {
        this(connection, "", arguments);
    }

    @Override
    public void execute()
        throws CacheException, IOException
    {
        switch (mode) {
            case FILE_MODE:
                importFile();
                break;
            case STREAM_MODE:
                importStream();
                break;
            default:
                throw new Error("Unreachable; how did I get there?");
        }
    }

    Set<String> importAndList()
        throws CacheException, IOException, SQLException
    {
        switch (mode) {
            case FILE_MODE:
                return importFile();
            case STREAM_MODE:
                return importStreamAndList();
            default:
                throw new Error("Unreachable; how did I get there?");
        }
    }

    private Set<String> importFile()
        throws CacheException, IOException
    {
        final Set<String> set = new HashSet<>();

        for (final Path path: files)
            set.addAll(runner.importFile(path, includeSys));

        return Collections.unmodifiableSet(set);
    }

    private Set<String> importStreamAndList()
        throws CacheException, SQLException, IOException
    {
        final Set<String> before = runner.listItems(includeSys);
        importStream();
        final Set<String> after = new HashSet<>(runner.listItems(includeSys));
        after.removeAll(before);
        return Collections.unmodifiableSet(after);
    }

    private void importStream()
        throws CacheException, IOException
    {
        for (final Path path: files)
            runner.importStream(path);
    }

    private static List<Path> collectPaths(final Path inputFile,
        final Path inputDir)
    {
        if (Stream.of(inputDir, inputFile).allMatch(Objects::isNull)) {
            System.err.println("Missing required argument (either inputFile"
                + " or inputDir must be specified)");
            System.exit(2);
            throw new Error("Unrechable! How did I get there?");
        }

        if (Stream.of(inputDir, inputFile).allMatch(Objects::nonNull)) {
            System.err.println("Only one of inputDir or inputFile can be"
                + " specified");
            System.exit(2);
            throw new Error("Unrechable! How did I get there?");
        }

        if (inputFile != null)
            return Collections.singletonList(inputFile);

        if (inputDir != null)
            try {
                return collectFiles(inputDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

        throw new Error("Unreachable! How did I get there?");
    }

    private static List<Path> collectFiles(final Path inputDir)
        throws IOException
    {
        final BiPredicate<Path, BasicFileAttributes> predicate
            = (path, attrs) -> attrs.isRegularFile() && path.getFileName()
            .toString().matches("(?i).*\\.(xml|ro)");
        try (
            final Stream<Path> stream = Files.find(inputDir, Integer.MAX_VALUE,
                predicate);
        ) {
            return stream.collect(Collectors.toList());
        }
    }

}
