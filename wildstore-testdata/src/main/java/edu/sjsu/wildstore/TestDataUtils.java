package edu.sjsu.wildstore;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TestDataUtils {
    private static FileSystem getFileSystemForUrl(URL uri) throws IOException {
        if (Set.of("jar", "zip").contains(uri.getProtocol())) {
            var bangIndex = uri.getPath().lastIndexOf('!');
            var jarUri = URI.create(uri.getPath().substring(0, bangIndex));
            System.out.println("getting file system for jar: " + jarUri);
            return FileSystems.newFileSystem(jarUri, Map.of());
        } else {
            return FileSystems.getDefault();
        }
    }
    public static void walkTestDataFiles(BiConsumer<Path, Path> consumer) throws IOException, URISyntaxException {
        var resourcesUrl = TestDataUtils.class.getResource("/testdata");

        var fs = getFileSystemForUrl(resourcesUrl);
        try {
            var path = fs.getPath(resourcesUrl.getPath());
            System.out.println("Walking test data files in: " + path);
            try (var pathStream = Files.walk(path)) {
                pathStream.filter(Files::isRegularFile)
                        .map(p -> new Path[] { p, path.relativize(p) })
                        .forEach(pa -> consumer.accept(pa[0], pa[1]));
            }
        } finally {
            if (fs == FileSystems.getDefault()) {
                // this hack brought to you by the devs that thought it would be a good idea
                // that FileSystems.default() is not autoclosable...
                return;
            }
            fs.close();
        }
    }
}
