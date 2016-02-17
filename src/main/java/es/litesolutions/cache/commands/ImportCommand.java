package es.litesolutions.cache.commands;

import com.intersys.objects.CacheException;
import es.litesolutions.cache.db.CacheDb;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    public ImportCommand(final CacheDb cacheDb,
        final Map<String, String> arguments)
    {
        super(cacheDb, arguments);

        final String inputFile = arguments.get(INPUT_FILE);
        final String inputDir = arguments.get(INPUT_DIR);

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
        final Set<String> before = runner.listClasses(includeSys);
        importStream();
        final Set<String> after = new HashSet<>(runner.listClasses(includeSys));
        after.removeAll(before);
        return Collections.unmodifiableSet(after);
    }

    private void importStream()
        throws CacheException, IOException
    {
        for (final Path path: files)
            runner.importStream(path);
    }

    private static List<Path> collectPaths(final String inputFile,
        final String inputDir)
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
            return Collections.singletonList(Paths.get(inputFile));

        if (inputDir != null)
            try {
                return collectFiles(inputDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

        throw new Error("Unreachable! How did I get there?");
    }

    private static List<Path> collectFiles(final String inputDir)
        throws IOException
    {
        final Path dir = Paths.get(inputDir).toRealPath();
        final BiPredicate<Path, BasicFileAttributes> predicate
            = (path, attrs) -> attrs.isRegularFile() && path.getFileName()
            .toString().toLowerCase().endsWith(".xml");
        try (
            final Stream<Path> stream = Files.find(dir, Integer.MAX_VALUE,
                predicate);
        ) {
            return stream.collect(Collectors.toList());
        }
    }

}
