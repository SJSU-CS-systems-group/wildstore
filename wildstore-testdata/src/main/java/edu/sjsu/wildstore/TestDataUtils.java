package edu.sjsu.wildstore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TestDataUtils {
    private static FileSystem getFileSystemForUrl(URL uri) throws IOException, URISyntaxException {
        if (Set.of("jar", "zip").contains(uri.getProtocol())) {
            var bangIndex = uri.getPath().lastIndexOf('!');
            var jarUri =uri.toURI();
            System.out.println("getting file system for jar: " + jarUri);
            return FileSystems.newFileSystem(jarUri, Map.of());
        } else {
            return FileSystems.getDefault();
        }
    }

    private static Path getPathForUrl(FileSystem fs, URL uri) throws IOException, URISyntaxException {
        if (Set.of("jar", "zip").contains(uri.getProtocol())) {
            var bangIndex = uri.getPath().lastIndexOf('!');
            var pathString = uri.getPath().substring(bangIndex+1);
            return Path.of(pathString);
        } else {
            return Path.of(uri.getPath());
        }
    }

    public static void extractTestData(Path destinationPath) throws IOException, URISyntaxException {
        var is = TestDataUtils.class.getResourceAsStream("/testdata.zip");
        if (is == null) {
            throw new IOException("Test data zip file /testdata.zip not found in classpath.");
        }
        var zipStream = new ZipInputStream(is);

        ZipEntry entry;
        while ((entry = zipStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                var entryPath = entry.getName();
                if (entryPath.startsWith("/")) {
                    entryPath = entryPath.substring(1);
                }
                var dst = destinationPath.resolve(entryPath);
                Files.createDirectories(dst.getParent());
                try (var os = Files.newOutputStream(dst)) {
                    zipStream.transferTo(os);
                }
            }
        }
    }
}
