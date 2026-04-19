package edu.sjsu.wildstore;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;

public class BasicFileReaderTest {

    @TempDir
    static Path tempDir;

    @Test
    void testProcessMetadataMissingFile() {
        Assertions.assertThrows(RuntimeException.class,
                () -> new BasicFileReader("nonexistent/path/file.txt").processMetadata());
    }

    @Test
    void testProcessMetadata() throws IOException {
        Path file = tempDir.resolve("sample.txt");
        Files.write(file, "hello world".getBytes());

        var reader = new BasicFileReader(file.toString());
        var metadata = reader.processMetadata();

        Assertions.assertNotNull(metadata);
        Assertions.assertTrue(metadata.fileName.contains(file.toString()));
        Assertions.assertEquals(1, metadata.filePath.size());
        Assertions.assertTrue(metadata.filePath.iterator().next().endsWith("/"));
        Assertions.assertTrue(metadata.fileSize > 0);
        Assertions.assertTrue(metadata.lastModified > 0);
        Assertions.assertNotNull(metadata.digestString);
        Assertions.assertTrue(metadata.globalAttributes.isEmpty());
    }

    @Test
    void testProcessFileContentsIsNoOp() throws IOException {
        Path file = tempDir.resolve("noop.txt");
        Files.write(file, "data".getBytes());

        var reader = new BasicFileReader(file.toString());
        var metadata = new Metadata();
        reader.processFileContents(metadata, 1000);

        Assertions.assertNull(metadata.fileName);
        Assertions.assertNull(metadata.globalAttributes);
    }

    @Test
    void testProcessFile() throws IOException {
        Path file = tempDir.resolve("processfile.txt");
        Files.write(file, "content".getBytes());

        var reader = new BasicFileReader(file.toString());
        var metadata = reader.processFile(1000);

        Assertions.assertNotNull(metadata);
        Assertions.assertTrue(metadata.fileName.contains(file.toString()));
        Assertions.assertNotNull(metadata.digestString);
    }
}